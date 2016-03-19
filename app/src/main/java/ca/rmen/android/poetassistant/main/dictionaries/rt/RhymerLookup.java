/*
 * Copyright (c) 2016 Carmen Alvarez
 *
 * This file is part of Poet Assistant.
 *
 * Poet Assistant is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Poet Assistant is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Poet Assistant.  If not, see <http://www.gnu.org/licenses/>.
 */

package ca.rmen.android.poetassistant.main.dictionaries.rt;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ca.rmen.android.poetassistant.Constants;
import ca.rmen.android.poetassistant.R;
import ca.rmen.rhymer.RhymeResult;

public class RhymerLookup {

    private static final String TAG = Constants.TAG + RhymerLookup.class.getSimpleName();

    private final Context mContext;
    private final String mQuery;
    private final String mFilter;

    public RhymerLookup(Context context, String query, String filter) {
        mContext = context.getApplicationContext();
        mQuery = query;
        mFilter = filter;
    }

    public List<RTEntry> lookup() {
        Log.d(TAG, "lookup() called with: query = " + mQuery + ", filter = " + mFilter);

        List<RTEntry> data = new ArrayList<>();
        Rhymer rhymer = Rhymer.getInstance(mContext);
        if (TextUtils.isEmpty(mQuery)) return data;

        List<RhymeResult> rhymeResults = rhymer.getRhymingWords(mQuery);
        if (rhymeResults == null) {
            return data;
        }
        if (!TextUtils.isEmpty(mFilter)) {
            Set<String> synonyms = Thesaurus.getInstance(mContext).getFlatSynonyms(mFilter);
            if (synonyms.isEmpty()) return data;
            rhymeResults = filter(rhymeResults, synonyms);
        }
        for (RhymeResult rhymeResult : rhymeResults) {
            // Add the word variant, if there are multiple pronunciations.
            if (rhymeResults.size() > 1) {
                String heading = mQuery + " (" + (rhymeResult.variantNumber + 1) + ")";
                data.add(new RTEntry(RTEntry.Type.HEADING, heading));
            }

            addResultSection(data, R.string.rhyme_section_stress_syllables, rhymeResult.strictRhymes);
            addResultSection(data, R.string.rhyme_section_one_syllable, rhymeResult.oneSyllableRhymes);
            addResultSection(data, R.string.rhyme_section_two_syllables, rhymeResult.twoSyllableRhymes);
            addResultSection(data, R.string.rhyme_section_three_syllables, rhymeResult.threeSyllableRhymes);
        }
        return data;
    }

    private void addResultSection(List<RTEntry> results, int sectionHeadingResId, String[] rhymes) {
        if (rhymes.length > 0) {
            results.add(new RTEntry(RTEntry.Type.SUBHEADING, mContext.getString(sectionHeadingResId)));
            for (String word : rhymes) {
                results.add(new RTEntry(RTEntry.Type.WORD, word));
            }
        }
    }

    private static List<RhymeResult> filter(List<RhymeResult> rhymes, Set<String> filter) {
        List<RhymeResult> filteredRhymes = new ArrayList<>();
        for (RhymeResult rhymeResult : rhymes) {
            RhymeResult filteredRhymeResult = filter(rhymeResult, filter);
            if (filteredRhymeResult != null) filteredRhymes.add(filteredRhymeResult);
        }
        return filteredRhymes;
    }

    private static RhymeResult filter(RhymeResult rhyme, Set<String> filter) {
        RhymeResult result = new RhymeResult(rhyme.variantNumber,
                RTUtils.filter(rhyme.strictRhymes, filter),
                RTUtils.filter(rhyme.oneSyllableRhymes, filter),
                RTUtils.filter(rhyme.twoSyllableRhymes, filter),
                RTUtils.filter(rhyme.threeSyllableRhymes, filter));
        if (isEmpty(result)) return null;
        return result;
    }

    private static boolean isEmpty(RhymeResult rhymeResult) {
        return rhymeResult.strictRhymes.length == 0
                && rhymeResult.oneSyllableRhymes.length == 0
                && rhymeResult.twoSyllableRhymes.length == 0
                && rhymeResult.threeSyllableRhymes.length == 0;
    }


}