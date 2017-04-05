package net.evilezh.mesosconsul.model.mesos;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RangeResource extends Resource {
    public List<Range> ranges;

    public void setRanges(Map<String, List<Map<String, Long>>> data) {
        if (data == null) {
            System.out.println("DATA = NULL");
        }
        ranges = data.get("range")
                .stream()
                .map(it -> {
            Range range = new Range();
            range.begin = Math.toIntExact(it.get("begin"));
            range.end = Math.toIntExact(it.get("end"));
            return range;
        }).collect(Collectors.toList());
    }
}
