package com.lordmau5.ffs.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServerTaskQueueTest {

    @BeforeEach
    void clearQueue() {
        ServerTaskQueue.drain();
    }

    @Test
    void drainsTasksInOrder() {
        List<Integer> order = new ArrayList<>();
        ServerTaskQueue.schedule(() -> order.add(1));
        ServerTaskQueue.schedule(() -> order.add(2));
        assertEquals(2, ServerTaskQueue.size());

        ServerTaskQueue.drain();

        assertEquals(Arrays.asList(1, 2), order);
        assertEquals(0, ServerTaskQueue.size());
    }

    @Test
    void ignoresNullTasks() {
        ServerTaskQueue.schedule(null);
        assertEquals(0, ServerTaskQueue.size());
    }

    @Test
    void aFailedTaskDoesNotBlockLaterTasks() {
        List<Integer> order = new ArrayList<>();
        ServerTaskQueue.schedule(() -> { throw new IllegalStateException("boom"); });
        ServerTaskQueue.schedule(() -> order.add(2));

        ServerTaskQueue.drain();

        assertTrue(order.contains(2));
        assertEquals(0, ServerTaskQueue.size());
    }
}
