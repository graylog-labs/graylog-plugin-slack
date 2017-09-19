package org.graylog2.plugins.slack;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

public class StringReplacementTest {

    @Test
    public void testExprNull() {
        String output = StringReplacement.replace(null, Collections.emptyMap());
        assertNull(output);
    }

    @Test
    public void testMapNull() {
        final String input = "input";
        String output = StringReplacement.replace(input, null);
        assertEquals(output, output);
    }

    @Test
    public void testNoExpr() {
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("source", "server1");
        final String input = "input";
        String output = StringReplacement.replace(input, fields);
        assertEquals(output, output);
    }

    @Test
    public void test1Expr() {
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("source", "server1");
        final String input = "${source}";
        String output = StringReplacement.replace(input, fields);
        assertEquals("server1", output);
    }

    @Test
    public void test2Expr() {
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("source", "server1");
        fields.put("user", "siri");
        final String input = "${source}${user}";
        String output = StringReplacement.replace(input, fields);
        assertEquals("server1siri", output);
    }

    @Test
    public void testMixExpr() {
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("source", "server1");
        fields.put("user", "siri");
        final String input = "From ${source} by ${user:-john}";
        String output = StringReplacement.replace(input, fields);
        assertEquals("From server1 by siri", output);
    }

    @Test
    public void testExprDefault() {
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("source", "server1");
        final String input = "From ${source:-} by ${user:-john}";
        String output = StringReplacement.replace(input, fields);
        assertEquals("From server1 by john", output);
    }

    @Test
    public void testExprDefaultEmpty() {
        Map<String, Object> fields = new HashMap<String, Object>();
        final String input = "${source:-}";
        String output = StringReplacement.replace(input, fields);
        assertEquals("", output);
    }

    @Test
    public void testPrefix() {
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("user", "siri");
        final String input = "${user:-}";
        String output = StringReplacement.replaceWithPrefix(input, "@", fields);
        assertEquals("@siri", output);
    }

    @Test
    public void testPrefixOnDefaultEmpty() {
        Map<String, Object> fields = new HashMap<String, Object>();
        final String input = "${user:-}";
        String output = StringReplacement.replaceWithPrefix(input, "@", fields);
        assertEquals("", output);
    }

    @Test
    public void testPrefixOnEmptyValue() {
        Map<String, Object> fields = new HashMap<String, Object>();
        final String input = "${user}";
        String output = StringReplacement.replaceWithPrefix(input, "@", fields);
        assertEquals("", output);
    }

}
