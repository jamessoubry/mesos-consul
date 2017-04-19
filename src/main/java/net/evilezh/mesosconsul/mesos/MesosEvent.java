package net.evilezh.mesosconsul.mesos;


import net.evilezh.mesosconsul.model.mesos.Task;

public interface MesosEvent {

    enum EVENT_TYPE {
        SUBSCRIBED_START("SUBSCRIBED_START"),
        SUBSCRIBED_END("SUBSCRIBED_END"),
        TASK_ADDED("TASK_ADDED"),
        TASK_UPDATED("TASK_UPDATED");

        private final String text;

        EVENT_TYPE(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    void mesosEvent(EVENT_TYPE eventType, Task task);
}
