package com.appdynamics.extensions.process.common;

import com.appdynamics.extensions.process.configuration.Instance;
import com.google.common.base.Charsets;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ProcessUtilTest {

    @Test
    public void testFilterProcessLinesFromCompleteList() throws IOException {
        List<String> processOutputList = Files.readLines(new File("src/test/resources/outputsamples/linux/ps.txt"), Charsets.UTF_8);
        List<Instance> instances = Lists.newArrayList();

        Instance instance = new Instance();
        instance.setDisplayName("java");
        instance.setRegex("java.exe");
        instances.add(instance);

        Instance instance1 = new Instance();
        instance.setDisplayName("ssh");
        instance.setRegex(".*sshd.*");
        instances.add(instance1);

        List<String> headerColumns = Lists.newArrayList();
        headerColumns.add("PID");
        headerColumns.add("%MEMORY");
        headerColumns.add("%CP");
        headerColumns.add("COMMAND");

        ListMultimap<String, String> filteredLines = ProcessUtil.filterProcessLinesFromCompleteList(processOutputList,instances, headerColumns);
        Assert.assertEquals(0, filteredLines.get("java").size());
        Assert.assertEquals(3, filteredLines.get("ssh").size());
    }

    @Test
    public void testStringSplitWithLimit() {
        String string = "  0.0   0.0 /sbin/init splash";
        String [] columns = string.trim().split("\\s+");
        Assert.assertNotEquals(3, columns.length);
        Assert.assertEquals("/sbin/init", columns[2]);

        String [] columns1 = string.trim().split("\\s+", 3);
        Assert.assertEquals(3, columns1.length);
        Assert.assertEquals("/sbin/init splash", columns1[2]);
    }

}
