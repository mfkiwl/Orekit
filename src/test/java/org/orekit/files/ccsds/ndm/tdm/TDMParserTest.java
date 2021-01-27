/* Copyright 2002-2021 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.files.ccsds.ndm.tdm;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
import org.orekit.files.ccsds.utils.lexical.KVNLexicalAnalyzer;
import org.orekit.files.ccsds.utils.lexical.LexicalAnalyzer;
import org.orekit.files.ccsds.utils.lexical.XMLLexicalAnalyzer;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;

/**
 * Test class for CCSDS Tracking Data Message parsing.<p>
 * Examples are taken from Annexe D of
 * <a href="https://public.ccsds.org/Pubs/503x0b1c1.pdf">CCSDS 503.0-B-1 recommended standard [1]</a> ("Tracking Data Message", Blue Book, Version 1.0, November 2007).<p>
 * Both KeyValue and XML formats are tested here on equivalent files.
 * @author mjournot
 *
 */
public class TDMParserTest {

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testParseTdmExternalResourceIssue368() {
        // setup
        String name = "/ccsds/tdm/xml/TDM-external-doctype.xml";
        LexicalAnalyzer lexicalAnalyzer =
                        new XMLLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name);

        try {
            // action
            lexicalAnalyzer.accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                 DataContext.getDefault(), null));

            // verify
            Assert.fail("Expected Exception");
        } catch (OrekitException e) {
            // Malformed URL exception indicates external resource was disabled
            // file not found exception indicates parser tried to load the resource
            MatcherAssert.assertThat(e.getCause(),
                    CoreMatchers.instanceOf(MalformedURLException.class));
        }
    }

    @Test
    public void testParseTdmKeyValueExample2() {
        // Example 2 of [1]
        // See Figure D-2: TDM Example: One-Way Data w/Frequency Offset
        // Data lines number was cut down to 7
        final String name = "/ccsds/tdm/kvn/TDMExample2.txt";
        LexicalAnalyzer lexicalAnalyzer =
                        new KVNLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name);
        final TDMFile file = lexicalAnalyzer.accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                                  DataContext.getDefault(), null));
        validateTDMExample2(file);
    }

    @Test
    public void testParseTdmKeyValueExample4() {

        // Example 4 of [1]
        // See Figure D-4: TDM Example: Two-Way Ranging Data Only
        // Data lines number was cut down to 20
        final String name = "/ccsds/tdm/kvn/TDMExample4.txt";
        LexicalAnalyzer lexicalAnalyzer =
                        new KVNLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name);
        final TDMFile file = lexicalAnalyzer.accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                                  DataContext.getDefault(), null));
        validateTDMExample4(file);
    }

    @Test
    public void testParseTdmKeyValueExample6() {

        // Example 6 of [1]
        // See Figure D-6: TDM Example: Four-Way Data
        // Data lines number was cut down to 16
        final String name = "/ccsds/tdm/kvn/TDMExample6.txt";
        LexicalAnalyzer lexicalAnalyzer =
                        new KVNLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name);
        final TDMFile file = lexicalAnalyzer.accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                                  DataContext.getDefault(), null));
        validateTDMExample6(file);
    }

    @Test
    public void testParseTdmKeyValueExample8() {

        // Example 8 of [1]
        // See Figure D-8: TDM Example: Angles, Range, Doppler Combined in Single TDM
        // Data lines number was cut down to 18
        final String name = "/ccsds/tdm/kvn/TDMExample8.txt";
        LexicalAnalyzer lexicalAnalyzer =
                        new KVNLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name);
        final TDMFile file = lexicalAnalyzer.accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                                  DataContext.getDefault(), null));
        validateTDMExample8(file);
    }

    @Test
    public void testParseTdmKeyValueExample15() {

        // Example 15 of [1]
        // See Figure D-15: TDM Example: Clock Bias/Drift Only
        final String name = "/ccsds/tdm/kvn/TDMExample15.txt";
        LexicalAnalyzer lexicalAnalyzer =
                        new KVNLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name);
        final TDMFile file = lexicalAnalyzer.accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                                  DataContext.getDefault(), null));
        validateTDMExample15(file);
    }

    @Test
    public void testParseTdmKeyValueExampleAllKeywords() {

        // Testing all TDM keywords
        final String name = "/ccsds/tdm/kvn/TDMExampleAllKeywords.txt";
        LexicalAnalyzer lexicalAnalyzer =
                        new KVNLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name);
        final TDMFile file = lexicalAnalyzer.accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                                  DataContext.getDefault(), null));
        validateTDMExampleAllKeywords(file);
    }

    @Test
    public void testParseTdmXmlExample2() {

        // Example 2 of [1]
        // See Figure D-2: TDM Example: One-Way Data w/Frequency Offset
        // Data lines number was cut down to 7
        final String name = "/ccsds/tdm/xml/TDMExample2.xml";
        LexicalAnalyzer lexicalAnalyzer =
                        new XMLLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name);
        final TDMFile file = lexicalAnalyzer.accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                                  DataContext.getDefault(), null));
        validateTDMExample2(file);
    }

    @Test
    public void testParseTdmXmlExample4() {

        // Example 4 of [1]
        // See Figure D-4: TDM Example: Two-Way Ranging Data Only
        // Data lines number was cut down to 20
        final String name = "/ccsds/tdm/xml/TDMExample4.xml";
        LexicalAnalyzer lexicalAnalyzer =
                        new XMLLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name);
        final TDMFile file = lexicalAnalyzer.accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                                  DataContext.getDefault(), null));
        validateTDMExample4(file);
    }

    @Test
    public void testParseTdmXmlExample6() {

        // Example 6 of [1]
        // See Figure D-6: TDM Example: Four-Way Data
        // Data lines number was cut down to 16
        final String name = "/ccsds/tdm/xml/TDMExample6.xml";
        LexicalAnalyzer lexicalAnalyzer =
                        new XMLLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name);
        final TDMFile file = lexicalAnalyzer.accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                                  DataContext.getDefault(), null));
        validateTDMExample6(file);
    }

    @Test
    public void testParseTdmXmlExample8() {

        // Example 8 of [1]
        // See Figure D-8: TDM Example: Angles, Range, Doppler Combined in Single TDM
        // Data lines number was cut down to 18
        final String name = "/ccsds/tdm/xml/TDMExample8.xml";
        LexicalAnalyzer lexicalAnalyzer =
                        new XMLLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name);
        final TDMFile file = lexicalAnalyzer.accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                                  DataContext.getDefault(), null));
        validateTDMExample8(file);
    }

    @Test
    public void testParseTdmXmlExample15() {

        // Example 15 of [1]
        // See Figure D-15: TDM Example: Clock Bias/Drift Only
        final String name = "/ccsds/tdm/xml/TDMExample15.xml";
        LexicalAnalyzer lexicalAnalyzer =
                        new XMLLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name);
        final TDMFile file = lexicalAnalyzer.accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                                  DataContext.getDefault(), null));
        validateTDMExample15(file);
    }

    @Test
    public void testParseTdmXmlExampleAllKeywords() {

        // Testing all TDM keywords
        final String name = "/ccsds/tdm/xml/TDMExampleAllKeywords.xml";
        LexicalAnalyzer lexicalAnalyzer =
                        new XMLLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name);
        final TDMFile file = lexicalAnalyzer.accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                                  DataContext.getDefault(), null));
        validateTDMExampleAllKeywords(file);
    }

    @Test
    public void testDataNumberFormatErrorTypeKeyValue() {
        try {
            // Number format exception in data part
            final String name = "/ccsds/tdm/kvn/TDM-data-number-format-error.txt";
            new KVNLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name).accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                                                                             DataContext.getDefault(), null));
            Assert.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            Assert.assertEquals("RECEIVE_FREQ_1", oe.getParts()[0]);
            Assert.assertEquals(26, oe.getParts()[1]);
            Assert.assertEquals("/ccsds/tdm/kvn/TDM-data-number-format-error.txt", oe.getParts()[2]);
        }
    }

    @Test
    public void testDataNumberFormatErrorTypeXml() {
        try {
            // Number format exception in data part
            final String name = "/ccsds/tdm/xml/TDM-data-number-format-error.xml";
            new XMLLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name).accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                                                                             DataContext.getDefault(), null));
            Assert.fail("An exception should have been thrown");
                    } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            Assert.assertEquals("RECEIVE_FREQ_1", oe.getParts()[0]);
            Assert.assertEquals(47, oe.getParts()[1]);
            Assert.assertEquals("/ccsds/tdm/xml/TDM-data-number-format-error.xml", oe.getParts()[2]);
        }
    }

    @Test
    public void testMetaDataNumberFormatErrorTypeKeyValue() {
        try {
            // Number format exception in metadata part
            final String name = "/ccsds/tdm/kvn/TDM-metadata-number-format-error.txt";
            new KVNLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name).accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                                                                             DataContext.getDefault(), null));
            Assert.fail("An Orekit Exception \"UNABLE_TO_PARSE_LINE_IN_FILE\" should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            Assert.assertEquals("TRANSMIT_DELAY_1", oe.getParts()[0]);
            Assert.assertEquals(17, oe.getParts()[1]);
            Assert.assertEquals("/ccsds/tdm/kvn/TDM-metadata-number-format-error.txt", oe.getParts()[2]);
        }
    }

    @Test
    public void testMetaDataNumberFormatErrorTypeXml() {
        try {
            // Number format exception in metadata part
            final String name = "/ccsds/tdm/xml/TDM-metadata-number-format-error.xml";
            new XMLLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name).accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                                                                             DataContext.getDefault(), null));
            Assert.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            Assert.assertEquals("TRANSMIT_DELAY_1", oe.getParts()[0]);
            Assert.assertEquals(24, oe.getParts()[1]);
            Assert.assertEquals("/ccsds/tdm/xml/TDM-metadata-number-format-error.xml", oe.getParts()[2]);
        }
    }

    @Test
    public void testNonExistentFile() throws URISyntaxException {
        // Try parsing a file that does not exist
        final String realName = getClass().getResource("/ccsds/odm/oem/OEMExample2.txt").toURI().getPath();
        final String wrongName = realName + "xxxxx";
        try {
            new KVNLexicalAnalyzer(wrongName);
            Assert.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_FIND_FILE, oe.getSpecifier());
            Assert.assertEquals(wrongName, oe.getParts()[0]);
        }
    }

    @Test
    public void testInconsistentTimeSystemsKeyValue() {
        // Inconsistent time systems between two sets of data
        try {
            final String name = "/ccsds/tdm/kvn/TDM-inconsistent-time-systems.txt";
            new KVNLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name).accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                                                                             DataContext.getDefault(), null));
            Assert.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_TDM_INCONSISTENT_TIME_SYSTEMS, oe.getSpecifier());
            Assert.assertEquals(CcsdsTimeScale.UTC, oe.getParts()[0]);
            Assert.assertEquals(CcsdsTimeScale.TCG, oe.getParts()[1]);
        }
    }

    @Test
    public void testInconsistentTimeSystemsXml() {
        // Inconsistent time systems between two sets of data
        try {
            final String name = "/ccsds/tdm/xml/TDM-inconsistent-time-systems.xml";
            new XMLLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name).accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                                                                             DataContext.getDefault(), null));
            Assert.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_TDM_INCONSISTENT_TIME_SYSTEMS, oe.getSpecifier());
            Assert.assertEquals(CcsdsTimeScale.UTC, oe.getParts()[0]);
            Assert.assertEquals(CcsdsTimeScale.TCG, oe.getParts()[1]);
        }
    }

    @Test
    public void testWrongDataKeywordKeyValue() throws URISyntaxException {
        // Unknown CCSDS keyword was read in data part
        final String name = "/ccsds/tdm/kvn/TDM-data-wrong-keyword.txt";
        final LexicalAnalyzer lexicalAnalyzer = new KVNLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name);
        try {
            lexicalAnalyzer.accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                 DataContext.getDefault(), null));
            Assert.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            Assert.assertEquals("WRONG_KEYWORD", oe.getParts()[0]);
            Assert.assertEquals(26, oe.getParts()[1]);
            Assert.assertEquals("%s","/ccsds/tdm/kvn/TDM-data-wrong-keyword.txt", oe.getParts()[2]);
        }
    }

    @Test
    public void testWrongDataKeywordXml() throws URISyntaxException {
        // Unknown CCSDS keyword was read in data part
        final String name = "/ccsds/tdm/xml/TDM-data-wrong-keyword.xml";
        final LexicalAnalyzer lexicalAnalyzer = new XMLLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name);
        try {
            lexicalAnalyzer.accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                 DataContext.getDefault(), null));
            Assert.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            Assert.assertEquals("WRONG_KEYWORD", oe.getParts()[0]);
            Assert.assertEquals(47, oe.getParts()[1]);
            Assert.assertEquals(name, oe.getParts()[2]);
        }
    }

    @Test
    public void testWrongMetaDataKeywordKeyValue() throws URISyntaxException {
        // Unknown CCSDS keyword was read in data part
        final String name = "/ccsds/tdm/kvn/TDM-metadata-wrong-keyword.txt";
        final LexicalAnalyzer lexicalAnalyzer = new KVNLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name);
        try {
            lexicalAnalyzer.accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                 DataContext.getDefault(), null));
            Assert.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            Assert.assertEquals("WRONG_KEYWORD", oe.getParts()[0]);
            Assert.assertEquals(16, oe.getParts()[1]);
            Assert.assertEquals("/ccsds/tdm/kvn/TDM-metadata-wrong-keyword.txt", oe.getParts()[2]);
        }
    }

    @Test
    public void testWrongMetaDataKeywordXml() throws URISyntaxException {
        // Unknown CCSDS keyword was read in data part
        final String name = "/ccsds/tdm/xml/TDM-metadata-wrong-keyword.xml";
        final LexicalAnalyzer lexicalAnalyzer = new XMLLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name);
        try {
            lexicalAnalyzer.accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                 DataContext.getDefault(), null));
            Assert.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            Assert.assertEquals("WRONG_KEYWORD", oe.getParts()[0]);
            Assert.assertEquals(23, oe.getParts()[1]);
            Assert.assertEquals("/ccsds/tdm/xml/TDM-metadata-wrong-keyword.xml", oe.getParts()[2]);
        }
    }

    @Test
    public void testWrongTimeSystemKeyValue() {
        // Time system not implemented CCSDS keyword was read in data part
        final String name = "/ccsds/tdm/kvn/TDM-metadata-timesystem-not-implemented.txt";
        final LexicalAnalyzer lexicalAnalyzer = new KVNLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name);
        try {
            lexicalAnalyzer.accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                 DataContext.getDefault(), null));
            Assert.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_TIME_SYSTEM_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals("WRONG-TIME-SYSTEM", oe.getParts()[0]);
        }
    }

    @Test
    public void testWrongTimeSystemXml() {
        // Time system not implemented CCSDS keyword was read in data part
        final String name = "/ccsds/tdm/xml/TDM-metadata-timesystem-not-implemented.xml";
        final LexicalAnalyzer lexicalAnalyzer = new XMLLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name);
        try {
            lexicalAnalyzer.accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                 DataContext.getDefault(), null));
            Assert.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_TIME_SYSTEM_NOT_IMPLEMENTED, oe.getSpecifier());
            Assert.assertEquals("WRONG-TIME-SYSTEM", oe.getParts()[0]);
        }
    }

    @Test
    public void testMissingTimeSystemXml() {
        // Time system not implemented CCSDS keyword was read in data part
        final String name = "/ccsds/tdm/xml/TDM-missing-timesystem.xml";
        final LexicalAnalyzer lexicalAnalyzer = new XMLLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name);
        try {
            lexicalAnalyzer.accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                 DataContext.getDefault(), null));
            Assert.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.CCSDS_TIME_SYSTEM_NOT_READ_YET, oe.getSpecifier());
            Assert.assertEquals(18, oe.getParts()[0]);
        }
    }

    @Test
    public void testInconsistentDataLineKeyValue() {
        // Inconsistent data line in KeyValue file (3 fields after keyword instead of 2)
        final String name = "/ccsds/tdm/kvn/TDM-data-inconsistent-line.txt";
        final LexicalAnalyzer lexicalAnalyzer = new KVNLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name);
        try {
            lexicalAnalyzer.accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                 DataContext.getDefault(), null));
            Assert.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            Assert.assertEquals("RECEIVE_FREQ_1", oe.getParts()[0]);
            Assert.assertEquals(25, oe.getParts()[1]);
            Assert.assertEquals("/ccsds/tdm/kvn/TDM-data-inconsistent-line.txt", oe.getParts()[2]);
        }
    }

    @Test
    public void testInconsistentDataBlockXml() {
        // Inconsistent data block in XML file
        final String name = "/ccsds/tdm/xml/TDM-data-inconsistent-block.xml";
        final LexicalAnalyzer lexicalAnalyzer = new XMLLexicalAnalyzer(TDMParserTest.class.getResourceAsStream(name), name);
        try {
            lexicalAnalyzer.accept(new TDMParser(IERSConventions.IERS_2010, true,
                                                 DataContext.getDefault(), null));
            Assert.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_ELEMENT_IN_FILE, oe.getSpecifier());
            Assert.assertEquals("TRANSMIT_FREQ_2", oe.getParts()[0]);
            Assert.assertEquals(32, oe.getParts()[1]);
            Assert.assertEquals("/ccsds/tdm/xml/TDM-data-inconsistent-block.xml", oe.getParts()[2]);
        }
    }

    /**
     * Validation function for example 2.
     * @param file Parsed TDMFile to validate
     */
    public static void validateTDMExample2(TDMFile file) {
        final TimeScale utc = TimeScalesFactory.getUTC();

        // Header
        Assert.assertEquals(1.0, file.getHeader().getFormatVersion(), 0.0);
        Assert.assertEquals(new AbsoluteDate("2005-160T20:15:00", utc).durationFrom(file.getHeader().getCreationDate()), 0.0, 0.0);
        Assert.assertEquals("NASA/JPL",file.getHeader().getOriginator());
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("TDM example created by yyyyy-nnnA Nav Team (NASA/JPL)");
        headerComment.add("StarTrek 1-way data, Ka band down");
        Assert.assertEquals(headerComment, file.getHeader().getComments());

        // Meta-Data
        final TDMMetadata metadata = file.getSegments().get(0).getMetadata();

        Assert.assertEquals(CcsdsTimeScale.UTC, metadata.getTimeSystem());
        Assert.assertEquals(new AbsoluteDate("2005-159T17:41:00", utc).durationFrom(metadata.getStartTime()), 0.0, 0.0);
        Assert.assertEquals(new AbsoluteDate("2005-159T17:41:40", utc).durationFrom(metadata.getStopTime()), 0.0, 0.0);
        Assert.assertEquals("DSS-25", metadata.getParticipants().get(1));
        Assert.assertEquals("yyyy-nnnA", metadata.getParticipants().get(2));
        Assert.assertEquals("SEQUENTIAL", metadata.getMode());
        Assert.assertEquals("2,1", metadata.getPath());
        Assert.assertEquals(1.0, metadata.getIntegrationInterval(), 0.0);
        Assert.assertEquals("MIDDLE", metadata.getIntegrationRef());
        Assert.assertEquals(32021035200.0, metadata.getFreqOffset(), 0.0);
        Assert.assertEquals(0.000077, metadata.getTransmitDelays().get(1), 0.0);
        Assert.assertEquals(0.000077, metadata.getReceiveDelays().get(1), 0.0);
        Assert.assertEquals("RAW", metadata.getDataQuality());
        final List<String> metaDataComment = new ArrayList<String>();
        metaDataComment.add("This is a meta-data comment");
        Assert.assertEquals(metaDataComment, metadata.getComments());

        // Data
        final List<Observation> observations = file.getSegments().get(0).getData().getObservations();

        // Reference data
        final String[] keywords = {"TRANSMIT_FREQ_2", "RECEIVE_FREQ_1", "RECEIVE_FREQ_1", "RECEIVE_FREQ_1",
            "RECEIVE_FREQ_1", "RECEIVE_FREQ_1", "RECEIVE_FREQ_1"};

        final String[] epochs   = {"2005-159T17:41:00", "2005-159T17:41:00", "2005-159T17:41:01", "2005-159T17:41:02",
            "2005-159T17:41:03", "2005-159T17:41:04", "2005-159T17:41:05"};

        final double[] values   = {32023442781.733, -409.2735, -371.1568, -333.0551,
            -294.9673, -256.9054, -218.7951};
        // Check consistency
        for (int i = 0; i < keywords.length; i++) {
            Assert.assertEquals(keywords[i], observations.get(i).getKeyword());
            Assert.assertEquals(new AbsoluteDate(epochs[i], utc).durationFrom(observations.get(i).getEpoch()), 0.0, 0.0);
            Assert.assertEquals(values[i], observations.get(i).getMeasurement(), 0.0);
        }

        // Comment
        final List<String> dataComment = new ArrayList<String>();
        dataComment.add("This is a data comment");
        Assert.assertEquals(dataComment, file.getSegments().get(0).getData().getComments());
    }

    /**
     * Validation function for example 4.
     * @param file Parsed TDMFile to validate
     */
    public static void validateTDMExample4(TDMFile file) {

        final TimeScale utc = TimeScalesFactory.getUTC();

        // Header
        Assert.assertEquals(1.0, file.getHeader().getFormatVersion(), 0.0);
        Assert.assertEquals(new AbsoluteDate("2005-191T23:00:00", utc).durationFrom(file.getHeader().getCreationDate()), 0.0, 0.0);
        Assert.assertEquals("NASA/JPL",file.getHeader().getOriginator());
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("TDM example created by yyyyy-nnnA Nav Team (NASA/JPL)");
        Assert.assertEquals(headerComment, file.getHeader().getComments());

        // Meta-Data
        final TDMMetadata metadata = file.getSegments().get(0).getMetadata();

        Assert.assertEquals(CcsdsTimeScale.UTC, metadata.getTimeSystem());
        Assert.assertEquals("DSS-24", metadata.getParticipants().get(1));
        Assert.assertEquals("yyyy-nnnA", metadata.getParticipants().get(2));
        Assert.assertEquals("SEQUENTIAL", metadata.getMode());
        Assert.assertEquals("1,2,1", metadata.getPath());
        Assert.assertEquals("START", metadata.getIntegrationRef());
        Assert.assertEquals("COHERENT", metadata.getRangeMode());
        Assert.assertEquals(2.0e+26, metadata.getRangeModulus(), 0.0);
        Assert.assertEquals("RU", metadata.getRangeUnits());
        Assert.assertEquals(7.7e-5, metadata.getTransmitDelays().get(1), 0.0);
        Assert.assertEquals(0.0, metadata.getTransmitDelays().get(2), 0.0);
        Assert.assertEquals(7.7e-5, metadata.getReceiveDelays().get(1), 0.0);
        Assert.assertEquals(0.0, metadata.getReceiveDelays().get(2), 0.0);
        Assert.assertEquals(46.7741, metadata.getCorrectionRange(), 0.0);
        Assert.assertEquals("YES", metadata.getCorrectionsApplied());
        final List<String> metaDataComment = new ArrayList<String>();
        metaDataComment.add("Range correction applied is range calibration to DSS-24.");
        metaDataComment.add("Estimated RTLT at begin of pass = 950 seconds");
        metaDataComment.add("Antenna Z-height correction 0.0545 km applied to uplink signal");
        metaDataComment.add("Antenna Z-height correction 0.0189 km applied to downlink signal");
        Assert.assertEquals(metaDataComment, metadata.getComments());

        // Data
        final List<Observation> observations = file.getSegments().get(0).getData().getObservations();

        // Reference data
        final String[] keywords = {"TRANSMIT_FREQ_1", "TRANSMIT_FREQ_RATE_1", "RANGE", "PR_N0",
            "TRANSMIT_FREQ_1", "TRANSMIT_FREQ_RATE_1", "RANGE", "PR_N0",
            "TRANSMIT_FREQ_1", "TRANSMIT_FREQ_RATE_1", "RANGE", "PR_N0",
            "TRANSMIT_FREQ_1", "TRANSMIT_FREQ_RATE_1", "RANGE", "PR_N0"};

        final String[] epochs   = {"2005-191T00:31:51", "2005-191T00:31:51", "2005-191T00:31:51", "2005-191T00:31:51",
            "2005-191T00:34:48", "2005-191T00:34:48", "2005-191T00:34:48", "2005-191T00:34:48",
            "2005-191T00:37:45", "2005-191T00:37:45", "2005-191T00:37:45", "2005-191T00:37:45",
            "2005-191T00:40:42", "2005-191T00:40:42", "2005-191T00:40:42", "2005-191T00:40:42",
            "2005-191T00:58:24", "2005-191T00:58:24", "2005-191T00:58:24", "2005-191T00:58:24"};

        final double[] values   = {7180064367.3536 , 0.59299, 39242998.5151986, 28.52538,
            7180064472.3146 , 0.59305, 61172265.3115234, 28.39347,
            7180064577.2756 , 0.59299, 15998108.8168328, 28.16193,
            7180064682.2366 , 0.59299, 37938284.4138008, 29.44597,
            7180065327.56141, 0.62085, 35478729.4012973, 30.48199};
        // Check consistency
        for (int i = 0; i < keywords.length; i++) {
            Assert.assertEquals(keywords[i], observations.get(i).getKeyword());
            Assert.assertEquals(new AbsoluteDate(epochs[i], utc).durationFrom(observations.get(i).getEpoch()), 0.0, 0.0);
            Assert.assertEquals(values[i], observations.get(i).getMeasurement(), 0.0);
        }
        // Comment
        final List<String> dataComment = new ArrayList<String>();
        dataComment.add("This is a data comment");
        Assert.assertEquals(dataComment, file.getSegments().get(0).getData().getComments());
    }

    /**
     * Validation function for example 6.
     * @param file Parsed TDMFile to validate
     */
    public static void validateTDMExample6(TDMFile file) {

        final TimeScale utc = TimeScalesFactory.getUTC();

        // Header
        Assert.assertEquals(1.0, file.getHeader().getFormatVersion(), 0.0);
        Assert.assertEquals(new AbsoluteDate("1998-06-10T01:00:00", utc).durationFrom(file.getHeader().getCreationDate()), 0.0, 0.0);
        Assert.assertEquals("JAXA",file.getHeader().getOriginator());
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("TDM example created by yyyyy-nnnA Nav Team (JAXA)");
        Assert.assertEquals(headerComment, file.getHeader().getComments());

        // Meta-Data
        final TDMMetadata metadata = file.getSegments().get(0).getMetadata();

        Assert.assertEquals(CcsdsTimeScale.UTC, metadata.getTimeSystem());
        Assert.assertEquals(new AbsoluteDate("1998-06-10T00:57:37", utc).durationFrom(metadata.getStartTime()), 0.0, 0.0);
        Assert.assertEquals(new AbsoluteDate("1998-06-10T00:57:44", utc).durationFrom(metadata.getStopTime()), 0.0, 0.0);
        Assert.assertEquals("NORTH", metadata.getParticipants().get(1));
        Assert.assertEquals("F07R07", metadata.getParticipants().get(2));
        Assert.assertEquals("E7", metadata.getParticipants().get(3));
        Assert.assertEquals("SEQUENTIAL", metadata.getMode());
        Assert.assertEquals("1,2,3,2,1", metadata.getPath());
        Assert.assertEquals(1.0, metadata.getIntegrationInterval(), 0.0);
        Assert.assertEquals("MIDDLE", metadata.getIntegrationRef());
        Assert.assertEquals("CONSTANT", metadata.getRangeMode());
        Assert.assertEquals(0.0, metadata.getRangeModulus(), 0.0);
        Assert.assertEquals("KM", metadata.getRangeUnits());
        Assert.assertEquals("AZEL", metadata.getAngleType());

        // Data
        final List<Observation> observations = file.getSegments().get(0).getData().getObservations();

        // Reference data
        final String[] keywords = {"RANGE", "ANGLE_1", "ANGLE_2", "TRANSMIT_FREQ_1", "RECEIVE_FREQ",
            "RANGE", "ANGLE_1", "ANGLE_2", "TRANSMIT_FREQ_1", "RECEIVE_FREQ",
            "RANGE", "ANGLE_1", "ANGLE_2", "TRANSMIT_FREQ_1", "RECEIVE_FREQ",
            "RANGE", "ANGLE_1", "ANGLE_2", "TRANSMIT_FREQ_1", "RECEIVE_FREQ",};

        final String[] epochs   = {"1998-06-10T00:57:37", "1998-06-10T00:57:37", "1998-06-10T00:57:37", "1998-06-10T00:57:37", "1998-06-10T00:57:37",
            "1998-06-10T00:57:38", "1998-06-10T00:57:38", "1998-06-10T00:57:38", "1998-06-10T00:57:38", "1998-06-10T00:57:38",
            "1998-06-10T00:57:39", "1998-06-10T00:57:39", "1998-06-10T00:57:39", "1998-06-10T00:57:39", "1998-06-10T00:57:39",
            "1998-06-10T00:57:44", "1998-06-10T00:57:44", "1998-06-10T00:57:44", "1998-06-10T00:57:44", "1998-06-10T00:57:44",};

        final double[] values   = { 80452.7542, 256.64002393, 13.38100016, 2106395199.07917, 2287487999.0,
            80452.7368, 256.64002393, 13.38100016, 2106395199.07917, 2287487999.0,
            80452.7197, 256.64002393, 13.38100016, 2106395199.07917, 2287487999.0,
            80452.6331, 256.64002393, 13.38100016, 2106395199.07917, 2287487999.0};
        // Check consistency
        for (int i = 0; i < keywords.length; i++) {
            Assert.assertEquals(keywords[i], observations.get(i).getKeyword());
            Assert.assertEquals(new AbsoluteDate(epochs[i], utc).durationFrom(observations.get(i).getEpoch()), 0.0, 0.0);
            Assert.assertEquals(values[i], observations.get(i).getMeasurement(), 0.0);
        }
        // Comment
        final List<String> dataComment = new ArrayList<String>();
        dataComment.add("This is a data comment");
        Assert.assertEquals(dataComment, file.getSegments().get(0).getData().getComments());
    }

    /**
     * Validation function for example 8.
     * @param file Parsed TDMFile to validate
     */
    public static void validateTDMExample8(TDMFile file) {

        final TimeScale utc = TimeScalesFactory.getUTC();

        // Header
        Assert.assertEquals(1.0, file.getHeader().getFormatVersion(), 0.0);
        Assert.assertEquals(new AbsoluteDate("2007-08-30T12:01:44.749", utc).durationFrom(file.getHeader().getCreationDate()), 0.0, 0.0);
        Assert.assertEquals("GSOC",file.getHeader().getOriginator());
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("GEOSCX_INP");
        Assert.assertEquals(headerComment, file.getHeader().getComments());

        // Meta-Data 1
        final TDMMetadata metadata = file.getSegments().get(0).getMetadata();

        Assert.assertEquals(CcsdsTimeScale.UTC, metadata.getTimeSystem());
        Assert.assertEquals(new AbsoluteDate("2007-08-29T07:00:02.000", utc).durationFrom(metadata.getStartTime()), 0.0, 0.0);
        Assert.assertEquals(new AbsoluteDate("2007-08-29T14:00:02.000", utc).durationFrom(metadata.getStopTime()), 0.0, 0.0);
        Assert.assertEquals("HBSTK", metadata.getParticipants().get(1));
        Assert.assertEquals("SAT", metadata.getParticipants().get(2));
        Assert.assertEquals("SEQUENTIAL", metadata.getMode());
        Assert.assertEquals("1,2,1", metadata.getPath());
        Assert.assertEquals(1.0, metadata.getIntegrationInterval(), 0.0);
        Assert.assertEquals("END", metadata.getIntegrationRef());
        Assert.assertEquals("XSYE", metadata.getAngleType());
        Assert.assertEquals("RAW", metadata.getDataQuality());
        final List<String> metaDataComment = new ArrayList<String>();
        metaDataComment.add("This is a meta-data comment");
        Assert.assertEquals(metaDataComment, metadata.getComments());

        // Data 1
        final List<Observation> observations = file.getSegments().get(0).getData().getObservations();

        // Reference data 1
        final String[] keywords = {"DOPPLER_INTEGRATED", "ANGLE_1", "ANGLE_2",
            "DOPPLER_INTEGRATED", "ANGLE_1", "ANGLE_2",
            "DOPPLER_INTEGRATED", "ANGLE_1", "ANGLE_2"};

        final String[] epochs   = {"2007-08-29T07:00:02.000", "2007-08-29T07:00:02.000", "2007-08-29T07:00:02.000",
            "2007-08-29T08:00:02.000", "2007-08-29T08:00:02.000", "2007-08-29T08:00:02.000",
            "2007-08-29T14:00:02.000", "2007-08-29T14:00:02.000", "2007-08-29T14:00:02.000"};

        final double[] values   = {-1.498776048, 67.01312389, 18.28395556,
            -2.201305217, 67.01982278, 21.19609167,
            0.929545817, -89.35626083, 2.78791667};
        // Check consistency
        for (int i = 0; i < keywords.length; i++) {
            Assert.assertEquals(keywords[i], observations.get(i).getKeyword());
            Assert.assertEquals(new AbsoluteDate(epochs[i], utc).durationFrom(observations.get(i).getEpoch()), 0.0, 0.0);
            Assert.assertEquals(values[i], observations.get(i).getMeasurement(), 0.0);
        }
        // Comment
        final List<String> dataComment = new ArrayList<String>();
        dataComment.add("This is a data comment");
        Assert.assertEquals(dataComment, file.getSegments().get(0).getData().getComments());

        // Meta-Data 2
        final TDMMetadata metadata2 = file.getSegments().get(1).getMetadata();

        Assert.assertEquals(CcsdsTimeScale.UTC, metadata2.getTimeSystem());
        Assert.assertEquals(new AbsoluteDate("2007-08-29T06:00:02.000", utc).durationFrom(metadata2.getStartTime()), 0.0, 0.0);
        Assert.assertEquals(new AbsoluteDate("2007-08-29T13:00:02.000", utc).durationFrom(metadata2.getStopTime()), 0.0, 0.0);
        Assert.assertEquals("WHM1", metadata2.getParticipants().get(1));
        Assert.assertEquals("SAT", metadata2.getParticipants().get(2));
        Assert.assertEquals("SEQUENTIAL", metadata2.getMode());
        Assert.assertEquals("1,2,1", metadata2.getPath());
        Assert.assertEquals(1.0, metadata2.getIntegrationInterval(), 0.0);
        Assert.assertEquals("END", metadata2.getIntegrationRef());
        Assert.assertEquals("AZEL", metadata2.getAngleType());
        Assert.assertEquals("RAW", metadata2.getDataQuality());
        final List<String> metaDataComment2 = new ArrayList<String>();
        metaDataComment2.add("This is a meta-data comment");
        Assert.assertEquals(metaDataComment2, metadata2.getComments());

        // Data 2
        final List<Observation> observations2 = file.getSegments().get(1).getData().getObservations();

        // Reference data 2
        final String[] keywords2 = {"RANGE", "DOPPLER_INTEGRATED", "ANGLE_1", "ANGLE_2",
            "RANGE", "DOPPLER_INTEGRATED", "ANGLE_1", "ANGLE_2",
            "RANGE", "DOPPLER_INTEGRATED", "ANGLE_1", "ANGLE_2"};

        final String[] epochs2   = {"2007-08-29T06:00:02.000", "2007-08-29T06:00:02.000", "2007-08-29T06:00:02.000", "2007-08-29T06:00:02.000",
            "2007-08-29T07:00:02.000", "2007-08-29T07:00:02.000", "2007-08-29T07:00:02.000", "2007-08-29T07:00:02.000",
            "2007-08-29T13:00:02.000", "2007-08-29T13:00:02.000", "2007-08-29T13:00:02.000", "2007-08-29T13:00:02.000"};

        final double[] values2   = {4.00165248953670E+04, -0.885640091,  99.53204250, 1.26724167,
            3.57238793591890E+04, -1.510223139, 103.33061750, 4.77875278,
            3.48156855860090E+04,  1.504082291, 243.73365222, 8.78254167};
        // Check consistency
        for (int i = 0; i < keywords2.length; i++) {
            Assert.assertEquals(keywords2[i], observations2.get(i).getKeyword());
            Assert.assertEquals(new AbsoluteDate(epochs2[i], utc).durationFrom(observations2.get(i).getEpoch()), 0.0, 0.0);
            Assert.assertEquals(values2[i], observations2.get(i).getMeasurement(), 0.0);
        }
        // Comment
        final List<String> dataComment2 = new ArrayList<String>();
        dataComment2.add("This is a data comment");
        Assert.assertEquals(dataComment2, file.getSegments().get(1).getData().getComments());
    }

    /**
     * Validation function for example 15.
     * @param file Parsed TDMFile to validate
     */
    public static void validateTDMExample15(TDMFile file) {

        final TimeScale utc = TimeScalesFactory.getUTC();

        // Header
        Assert.assertEquals(1.0, file.getHeader().getFormatVersion(), 0.0);
        Assert.assertEquals(new AbsoluteDate("2005-161T15:45:00", utc).durationFrom(file.getHeader().getCreationDate()), 0.0, 0.0);
        Assert.assertEquals("NASA/JPL",file.getHeader().getOriginator());
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("TDM example created by yyyyy-nnnA Nav Team (NASA/JPL)");
        headerComment.add("The following are clock offsets, in seconds between the");
        headerComment.add("clocks at each DSN complex relative to UTC(NIST). The offset");
        headerComment.add("is a mean of readings using several GPS space vehicles in");
        headerComment.add("common view. Value is \"station clock minus UTC”.");
        Assert.assertEquals(headerComment, file.getHeader().getComments());

        // Meta-Data 1
        final TDMMetadata metadata = file.getSegments().get(0).getMetadata();

        Assert.assertEquals(CcsdsTimeScale.UTC, metadata.getTimeSystem());
        Assert.assertEquals(new AbsoluteDate("2005-142T12:00:00", utc).durationFrom(metadata.getStartTime()), 0.0, 0.0);
        Assert.assertEquals(new AbsoluteDate("2005-145T12:00:00", utc).durationFrom(metadata.getStopTime()), 0.0, 0.0);
        Assert.assertEquals("DSS-10", metadata.getParticipants().get(1));
        Assert.assertEquals("UTC-NIST", metadata.getParticipants().get(2));
        final List<String> metaDataComment = new ArrayList<String>();
        metaDataComment.add("Note: SPC10 switched back to Maser1 from Maser2 on 2005-142");
        Assert.assertEquals(metaDataComment, metadata.getComments());

        // Data 1
        final List<Observation> observations = file.getSegments().get(0).getData().getObservations();

        // Reference data 1
        final String[] keywords = {"CLOCK_BIAS", "CLOCK_DRIFT",
            "CLOCK_BIAS", "CLOCK_DRIFT",
            "CLOCK_BIAS", "CLOCK_DRIFT",
        "CLOCK_BIAS"};

        final String[] epochs   = {"2005-142T12:00:00", "2005-142T12:00:00",
            "2005-143T12:00:00", "2005-143T12:00:00",
            "2005-144T12:00:00", "2005-144T12:00:00",
        "2005-145T12:00:00"};

        final double[] values   = {9.56e-7,  6.944e-14,
            9.62e-7, -2.083e-13,
            9.44e-7, -2.778e-13,
            9.20e-7};
        // Check consistency
        for (int i = 0; i < keywords.length; i++) {
            Assert.assertEquals(keywords[i], observations.get(i).getKeyword());
            Assert.assertEquals(new AbsoluteDate(epochs[i], utc).durationFrom(observations.get(i).getEpoch()), 0.0, 0.0);
            Assert.assertEquals(values[i], observations.get(i).getMeasurement(), 0.0);
        }
        // Comment
        final List<String> dataComment = new ArrayList<String>();
        dataComment.add("This is a data comment");
        Assert.assertEquals(dataComment, file.getSegments().get(0).getData().getComments());


        // Meta-Data 2
        final TDMMetadata metadata2 = file.getSegments().get(1).getMetadata();

        Assert.assertEquals(CcsdsTimeScale.UTC, metadata2.getTimeSystem());
        Assert.assertEquals(new AbsoluteDate("2005-142T12:00:00", utc).durationFrom(metadata2.getStartTime()), 0.0, 0.0);
        Assert.assertEquals(new AbsoluteDate("2005-145T12:00:00", utc).durationFrom(metadata2.getStopTime()), 0.0, 0.0);
        Assert.assertEquals("DSS-40", metadata2.getParticipants().get(1));
        Assert.assertEquals("UTC-NIST", metadata2.getParticipants().get(2));
        final List<String> metaDataComment2 = new ArrayList<String>();
        metaDataComment2.add("This is a meta-data comment");
        Assert.assertEquals(metaDataComment2, metadata2.getComments());

        // Data 2
        final List<Observation> observations2 = file.getSegments().get(1).getData().getObservations();

        // Reference data 2
        // Same keywords and dates than 1
        final double[] values2   = {-7.40e-7, -3.125e-13,
            -7.67e-7, -1.620e-13,
            -7.81e-7, -4.745e-13,
            -8.22e-7};
        // Check consistency
        for (int i = 0; i < keywords.length; i++) {
            Assert.assertEquals(keywords[i], observations2.get(i).getKeyword());
            Assert.assertEquals(new AbsoluteDate(epochs[i], utc).durationFrom(observations2.get(i).getEpoch()), 0.0, 0.0);
            Assert.assertEquals(values2[i], observations2.get(i).getMeasurement(), 0.0);
        }
        // Comment
        final List<String> dataComment2 = new ArrayList<String>();
        dataComment2.add("This is a data comment");
        Assert.assertEquals(dataComment2, file.getSegments().get(1).getData().getComments());


        // Meta-Data 3
        final TDMMetadata metadata3 = file.getSegments().get(2).getMetadata();

        Assert.assertEquals(CcsdsTimeScale.UTC, metadata3.getTimeSystem());
        Assert.assertEquals(new AbsoluteDate("2005-142T12:00:00", utc).durationFrom(metadata3.getStartTime()), 0.0, 0.0);
        Assert.assertEquals(new AbsoluteDate("2005-145T12:00:00", utc).durationFrom(metadata3.getStopTime()), 0.0, 0.0);
        Assert.assertEquals("DSS-60", metadata3.getParticipants().get(1));
        Assert.assertEquals("UTC-NIST", metadata3.getParticipants().get(2));
        final List<String> metaDataComment3 = new ArrayList<String>();
        metaDataComment3.add("This is a meta-data comment");
        Assert.assertEquals(metaDataComment3, metadata3.getComments());

        // Data 3
        final List<Observation> observations3 = file.getSegments().get(2).getData().getObservations();

        // Reference data 2
        // Same keywords and dates than 1
        final double[] values3   = {-1.782e-6, 1.736e-13,
            -1.767e-6, 1.157e-14,
            -1.766e-6, 8.102e-14,
            -1.759e-6};
        // Check consistency
        for (int i = 0; i < keywords.length; i++) {
            Assert.assertEquals(keywords[i], observations3.get(i).getKeyword());
            Assert.assertEquals(new AbsoluteDate(epochs[i], utc).durationFrom(observations3.get(i).getEpoch()), 0.0, 0.0);
            Assert.assertEquals(values3[i], observations3.get(i).getMeasurement(), 0.0);
        }
        // Comment
        final List<String> dataComment3 = new ArrayList<String>();
        dataComment3.add("This is a data comment");
        Assert.assertEquals(dataComment3, file.getSegments().get(2).getData().getComments());
    }

    /**
     * Validation function for example displaying all keywords.
     * @param file Parsed TDMFile to validate
     */
    public static void validateTDMExampleAllKeywords(TDMFile file) {

        final TimeScale utc = TimeScalesFactory.getUTC();

        // Header
        Assert.assertEquals(1.0, file.getHeader().getFormatVersion(), 0.0);
        Assert.assertEquals(new AbsoluteDate("2017-06-14T10:53:00.000", utc).durationFrom(file.getHeader().getCreationDate()), 0.0, 0.0);
        Assert.assertEquals("CSSI",file.getHeader().getOriginator());
        final List<String> headerComment = new ArrayList<String>();
        headerComment.add("TDM example created by CSSI");
        headerComment.add("Testing all TDM known meta-data and data keywords");
        Assert.assertEquals(headerComment, file.getHeader().getComments());

        // Meta-Data
        final TDMMetadata metadata = file.getSegments().get(0).getMetadata();

        Assert.assertEquals(CcsdsTimeScale.UTC, metadata.getTimeSystem());
        Assert.assertEquals(new AbsoluteDate("2017-06-14T10:53:00.000", utc).durationFrom(metadata.getStartTime()), 0.0, 0.0);
        Assert.assertEquals(new AbsoluteDate("2017-06-15T10:53:00.000", utc).durationFrom(metadata.getStopTime()), 0.0, 0.0);
        Assert.assertEquals("DSS-25", metadata.getParticipants().get(1));
        Assert.assertEquals("yyyy-nnnA", metadata.getParticipants().get(2));
        Assert.assertEquals("P3", metadata.getParticipants().get(3));
        Assert.assertEquals("P4", metadata.getParticipants().get(4));
        Assert.assertEquals("P5", metadata.getParticipants().get(5));
        Assert.assertEquals("SEQUENTIAL", metadata.getMode());
        Assert.assertEquals("2,1", metadata.getPath());
        Assert.assertEquals("4,5", metadata.getPath1());
        Assert.assertEquals("3,2", metadata.getPath2());
        Assert.assertEquals("S", metadata.getTransmitBand());
        Assert.assertEquals("L", metadata.getReceiveBand());
        Assert.assertEquals(240, metadata.getTurnaroundNumerator(), 0);
        Assert.assertEquals(221, metadata.getTurnaroundDenominator(), 0);
        Assert.assertEquals("TRANSMIT", metadata.getTimetagRef());
        Assert.assertEquals(1.0, metadata.getIntegrationInterval(), 0.0);
        Assert.assertEquals("MIDDLE", metadata.getIntegrationRef());
        Assert.assertEquals(32021035200.0, metadata.getFreqOffset(), 0.0);
        Assert.assertEquals("COHERENT", metadata.getRangeMode());
        Assert.assertEquals(32768.0, metadata.getRangeModulus(), 0.0);
        Assert.assertEquals("RU", metadata.getRangeUnits());
        Assert.assertEquals("RADEC", metadata.getAngleType());
        Assert.assertEquals("EME2000", metadata.getReferenceFrame().getName());
        Assert.assertEquals(true,FramesFactory.getEME2000().equals(metadata.getReferenceFrame()));
        Assert.assertEquals(0.000077, metadata.getTransmitDelays().get(1), 0.0);
        Assert.assertEquals(0.000077, metadata.getTransmitDelays().get(2), 0.0);
        Assert.assertEquals(0.000077, metadata.getTransmitDelays().get(3), 0.0);
        Assert.assertEquals(0.000077, metadata.getTransmitDelays().get(4), 0.0);
        Assert.assertEquals(0.000077, metadata.getTransmitDelays().get(5), 0.0);
        Assert.assertEquals(0.000077, metadata.getReceiveDelays().get(1), 0.0);
        Assert.assertEquals(0.000077, metadata.getReceiveDelays().get(2), 0.0);
        Assert.assertEquals(0.000077, metadata.getReceiveDelays().get(3), 0.0);
        Assert.assertEquals(0.000077, metadata.getReceiveDelays().get(4), 0.0);
        Assert.assertEquals(0.000077, metadata.getReceiveDelays().get(5), 0.0);
        Assert.assertEquals("RAW", metadata.getDataQuality());
        Assert.assertEquals(1.0, metadata.getCorrectionAngle1(), 0.0);
        Assert.assertEquals(2.0, metadata.getCorrectionAngle2(), 0.0);
        Assert.assertEquals(3.0, metadata.getCorrectionDoppler(), 0.0);
        Assert.assertEquals(4.0, metadata.getCorrectionRange(), 0.0);
        Assert.assertEquals(5.0, metadata.getCorrectionReceive(), 0.0);
        Assert.assertEquals(6.0, metadata.getCorrectionTransmit(), 0.0);
        Assert.assertEquals("YES", metadata.getCorrectionsApplied());

        final List<String> metaDataComment = new ArrayList<String>();
        metaDataComment.add("All known meta-data keywords displayed");
        Assert.assertEquals(metaDataComment, metadata.getComments());

        // Data
        final List<Observation> observations = file.getSegments().get(0).getData().getObservations();

        // Reference data
        final String[] keywords = {"CARRIER_POWER", "DOPPLER_INSTANTANEOUS", "DOPPLER_INTEGRATED", "PC_N0", "PR_N0", "RANGE",
                                   "RECEIVE_FREQ_1", "RECEIVE_FREQ_2", "RECEIVE_FREQ_3", "RECEIVE_FREQ_4", "RECEIVE_FREQ_5", "RECEIVE_FREQ",
                                   "TRANSMIT_FREQ_1", "TRANSMIT_FREQ_2", "TRANSMIT_FREQ_3", "TRANSMIT_FREQ_4","TRANSMIT_FREQ_5",
                                   "TRANSMIT_FREQ_RATE_1", "TRANSMIT_FREQ_RATE_2", "TRANSMIT_FREQ_RATE_3", "TRANSMIT_FREQ_RATE_4", "TRANSMIT_FREQ_RATE_5",
                                   "DOR", "VLBI_DELAY",
                                   "ANGLE_1", "ANGLE_2",
                                   "CLOCK_BIAS", "CLOCK_DRIFT",
                                   "STEC", "TROPO_DRY", "TROPO_WET",
                                   "PRESSURE", "RHUMIDITY", "TEMPERATURE"};

        final AbsoluteDate epoch = new AbsoluteDate("2017-06-14T10:53:00.000", utc);
        // Check consistency
        for (int i = 0; i < keywords.length; i++) {
            Assert.assertEquals(keywords[i], observations.get(i).getKeyword());
            Assert.assertEquals(epoch.shiftedBy((double) (i+1)).durationFrom(observations.get(i).getEpoch()), 0.0, 0.0);
            Assert.assertEquals((double) (i+1), observations.get(i).getMeasurement(), 0.0);
        }

        // Comment
        final List<String> dataComment = new ArrayList<String>();
        dataComment.add("Signal related Keywords");
        dataComment.add("VLBI/Delta-DOR Related Keywords");
        dataComment.add("Angle Related Keywords");
        dataComment.add("Time Related Keywords");
        dataComment.add("Media Related Keywords");
        dataComment.add("Meteorological Related Keywords");
        Assert.assertEquals(dataComment, file.getSegments().get(0).getData().getComments());
    }

}
