package com.appdynamics.extensions.process.parser;

import org.junit.Assert;
import org.junit.Test;

public class ParserFactoryTest {

    @Test
    public void testCreateLinuxParser() {
        Parser parser = ParserFactory.createParser("linux");
        Assert.assertTrue(parser instanceof LinuxParser);
    }

    @Test
    public void testCreateWindowsParser() {
        Parser parser = ParserFactory.createParser("win");
        Assert.assertTrue(parser instanceof WindowsParser);
    }

    @Test
    public void testCreateSolarisParser() {
        Parser parser = ParserFactory.createParser("sunos");
        Assert.assertTrue(parser instanceof SolarisParser);
    }

    @Test
    public void testCreateAIXParser() {
        Parser parser = ParserFactory.createParser("aix");
        Assert.assertTrue(parser instanceof AIXParser);
    }

    @Test
    public void testCreateHPUXParser() {
        Parser parser = ParserFactory.createParser("hp-ux");
        Assert.assertTrue(parser instanceof HPUXParser);
    }

    @Test
    public void testForNullParser() {
        Parser parser = ParserFactory.createParser("abc");
        Assert.assertNull(parser);
    }
}
