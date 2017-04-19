package net.evilezh.mesosconsul.transform;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import net.evilezh.mesosconsul.model.config.*;
import net.evilezh.mesosconsul.model.config.Transform;
import net.evilezh.mesosconsul.model.mesos.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Regex extends AbstractTranform {
    private static final Logger logger = LogManager.getLogger(Regex.class);
    private static Pattern varExtract = Pattern.compile("(?<=\\$\\{)[^}]+(?=})");
    private static ObjectMapper mapper = new ObjectMapper();
    private static final String PORT_PREFIX = "port-";
    private final RegexTransform rt;

    public Regex(String name, Transform config) throws Throwable {
        super(name, config);
        rt = mapper.readValue(mapper.writeValueAsBytes(config.config), RegexTransform.class);
    }

    private String getField(String fieldRef, Map<Match, Matcher> extract) {
        if (fieldRef.contains(".")) { //it is regex subgroup
            String[] split = fieldRef.split("\\.");
            Optional<Map.Entry<Match, Matcher>> a = extract.entrySet().stream().filter(entry -> entry.getKey().name.equals(split[0])).findFirst();
            if (a.isPresent()) {
                Matcher value = a.get().getValue();
                return value.group(split[1]);
            } else {
                throw new RuntimeException("Invalid field name requested: " + split[0]);
            }
        } else { //all field name
            Optional<Map.Entry<Match, Matcher>> a = extract.entrySet().stream().filter(entry -> entry.getKey().name.equals(fieldRef)).findFirst();
            if (a.isPresent()) {
                Matcher value = a.get().getValue();
                return value.group();
            } else {
                throw new RuntimeException("Invalid field name requested: " + fieldRef);
            }
        }

    }

    private String doEval(Target target, String portName, Map<Match, Matcher> extract) {
        StringBuilder sb = new StringBuilder();
        Matcher matcher = varExtract.matcher(target.task);
        int pos = 0;
        while (matcher.find()) {
            sb.append(target.task.substring(pos, matcher.start() - 2));
            String fieldRef = target.task.substring(matcher.start(), matcher.end()).trim();
            if (fieldRef.equals("_port-name_")) {
                sb.append(portName);
            } else {
                sb.append(getField(fieldRef, extract));
            }
            pos = matcher.end() + 1;
        }
        sb.append(target.task.substring(pos));
        return sb.toString();
    }

    @Override
    synchronized public List<TransformResult> transform(Task task, Agent agent, String servicePrefix) {
//        final Map<String, Integer> ret = new HashMap<>();
        final List<TransformResult> ret = new ArrayList<>();
        //RegexTransform rt = (RegexTransform) transform;
        Map<Match, Matcher> extract = new HashMap<>();
        boolean isMatch = false;
        for (Match match : rt.expression.match) {
            try {
                String value = JsonPath.read(mapper.writeValueAsString(task), match.jsonPath);
                Matcher matcher = match.regex.matcher(value);
                if (!matcher.matches()) {
                    extract.clear();
                    if (rt.expression.operator.equals("and"))
                        isMatch = false;
                    break;
                } else {
                    extract.put(match, matcher);
                    isMatch = true;
                }
            } catch (PathNotFoundException pe) {
                isMatch = false;
                break;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        if (isMatch) {
            for (Target target : rt.target) {
                if (task.discovery != null && task.discovery.ports != null) {
                    for (Port port : task.discovery.ports) {
                        int portIdx = 0;
                        try {
                            //extract port name
                            StringBuilder sb = new StringBuilder();
                            int pos = 0;
                            Matcher matcher = varExtract.matcher(target.port);
                            while (matcher.find()) {
                                sb.append(target.port.substring(pos, matcher.start() - 2));
                                String fieldRef = target.port.substring(matcher.start(), matcher.end()).trim();
                                if (fieldRef.startsWith("$")) { //json path
                                    String namePart = JsonPath.read(mapper.writeValueAsString(port), fieldRef);
                                    if (namePart == null || namePart.equals("null")) {
                                        sb.append(PORT_PREFIX + portIdx);
                                        portIdx++;
                                    } else {
                                        sb.append(namePart);
                                    }
                                } else { //parsed variable
                                    sb.append(getField(fieldRef, extract));
                                }
                                pos = matcher.end() + 1;
                            }
                            sb.append(target.port.substring(pos));
                            String portName = sb.toString();
                            TransformResult tr = new TransformResult();
                            tr.agentId = agent.agentInfo.id;
                            tr.port = port.number;
                            tr.portName = portName;
                            tr.transformName = this.name;
                            tr.servicePrefix = servicePrefix;
                            tr.taskName = doEval(target, portName, extract);
                            tr.taskId = task.taskId;
                            ret.add(tr);
//                            ret.put(doEval(target, portName, extract), port.number);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }

                    }
                } else {
                    if (task.resources.size() > 0) {
                        task.resources.stream().filter(it -> it.name.equals("ports")).findFirst().ifPresent(port -> {
                            RangeResource portRanges = (RangeResource) port;

                            AtomicInteger portIdx = new AtomicInteger(0);
                            ret.addAll(portRanges.ranges.stream()
                                    .flatMap(it -> IntStream.rangeClosed(it.begin, it.end).boxed())
                                    .map(it -> {
                                        TransformResult tr = new TransformResult();
                                        tr.portName = PORT_PREFIX + portIdx.getAndIncrement();
                                        tr.taskName = doEval(target, PORT_PREFIX + portIdx.getAndIncrement(), extract);
                                        tr.agentId = agent.agentInfo.id;
                                        tr.port = it;
                                        tr.transformName = this.name;
                                        tr.servicePrefix = servicePrefix;
                                        tr.taskId = task.taskId;
                                        doEval(target, PORT_PREFIX + portIdx.getAndDecrement(), extract);
                                        return tr;
                                    })
                                    .collect(Collectors.toList()));
                        });
                    }
                }
            }

        }
        return ret;
    }

    private String extract(String source, Map<String, Matcher> vars) {
        return "";
    }
}
