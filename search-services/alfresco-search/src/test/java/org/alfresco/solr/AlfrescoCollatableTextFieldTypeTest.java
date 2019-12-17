/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.solr;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.text.Collator;
import java.util.Locale;

import org.alfresco.solr.AlfrescoCollatableTextFieldType.TextSortFieldComparator;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

/** Unit tests for {@link AlfrescoCollatableTextFieldType}. */
public class AlfrescoCollatableTextFieldTypeTest
{
    private static final int NUM_HITS = 3;
    private static final String FIELD = "field";
    private static final Locale LOCALE = Locale.getDefault();
    /** A document id. */
    private static final int DOC = 0;
    /** A value for the current bottom document. */
    private static final String BOTTOM_STRING = "Bottom";

    @InjectMocks
    TextSortFieldComparator textSortFieldComparator = new TextSortFieldComparator(NUM_HITS, FIELD, LOCALE);
    @Mock
    BinaryDocValues mockDocTerms;
    @Mock
    Bits mockDocsWithField;
    @Mock
    Collator mockCollator;

    @Before
    public void setUp()
    {
        initMocks(this);
        reset(mockDocTerms, mockDocsWithField);
        textSortFieldComparator.bottom = BOTTOM_STRING;
    }

    /** Check that a zero length term is sorted before a populated field. */
    @Test
    public void testCompareBottom_termLengthZeroAndDocDoesntHaveField()
    {
        // Set up the document to have an empty term.
        when(mockDocTerms.get(DOC)).thenReturn(new BytesRef());
        when(mockDocsWithField.get(DOC)).thenReturn(false);

        // Call the method under test.
        int result = textSortFieldComparator.compareBottom(DOC);

        assertEquals("Expected value for doc to be null, and so it to be sorted before BOTTOM_TERM", 1, result);
    }

    /**
     * Check the behaviour of compareBottom when docsWithField is null (this happens when all documents contain the
     * field).
     */
    @Test
    public void testCompareBottom_nullDocsWithField()
    {
        // Set docsWithField to null to simulate all documents containing the field.
        Bits oldValue = textSortFieldComparator.docsWithField;
        textSortFieldComparator.docsWithField = null;

        // Set up the document to have an empty term.
        when(mockDocTerms.get(DOC)).thenReturn(new BytesRef());

        // Call the method under test.
        textSortFieldComparator.compareBottom(DOC);

        // Expect the EMPTY_TERM to be compared
        verify(mockCollator).compare(BOTTOM_STRING, "");

        // Reset docsWithField with the mock after the test.
        textSortFieldComparator.docsWithField = oldValue;
    }

    /** Check that if the doc has a value then it is compared with the existing value. */
    @Test
    public void testCompareBottom_populatedTerm()
    {
        // Set up the document to have "Some value" for the field.
        when(mockDocTerms.get(DOC)).thenReturn(new BytesRef("Some value"));
        when(mockDocsWithField.get(DOC)).thenReturn(false);

        // Call the method under test.
        textSortFieldComparator.compareBottom(DOC);

        verify(mockCollator).compare(BOTTOM_STRING, "Some value");
    }

    /** Check the behaviour if the term is encoded. */
    @Test
    public void testCompareBottom_encodedTerm()
    {
        // Set up the document to have an encoded value for the field.
        when(mockDocTerms.get(DOC)).thenReturn(new BytesRef("\u0000Value\u0000Ignored"));
        when(mockDocsWithField.get(DOC)).thenReturn(false);

        // Call the method under test.
        textSortFieldComparator.compareBottom(DOC);

        verify(mockCollator).compare(BOTTOM_STRING, "Value");
    }
}
