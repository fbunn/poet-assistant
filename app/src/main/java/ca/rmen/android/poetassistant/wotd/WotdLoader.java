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

package ca.rmen.android.poetassistant.wotd;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.text.format.DateUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;

import javax.inject.Inject;

import ca.rmen.android.poetassistant.Constants;
import ca.rmen.android.poetassistant.DaggerHelper;
import ca.rmen.android.poetassistant.R;
import ca.rmen.android.poetassistant.main.dictionaries.Favorites;
import ca.rmen.android.poetassistant.main.dictionaries.ResultListData;
import ca.rmen.android.poetassistant.main.dictionaries.ResultListLoader;
import ca.rmen.android.poetassistant.main.dictionaries.dictionary.Dictionary;

public class WotdLoader extends ResultListLoader<ResultListData<WotdEntry>> {

    private static final String TAG = Constants.TAG + WotdLoader.class.getSimpleName();

    @Inject
    Dictionary mDictionary;

    public WotdLoader(Context context) {
        super(context);
        DaggerHelper.getAppComponent(context).inject(this);
    }

    @Override
    public ResultListData<WotdEntry> loadInBackground() {
        Log.d(TAG, "loadInBackground()");

        List<WotdEntry> data = new ArrayList<>(100);

        Cursor cursor = mDictionary.getRandomWordCursor();
        if (cursor == null || cursor.getCount() == 0) return emptyResult();

        try {
            Set<String> favorites = new Favorites(getContext()).getFavorites();
            Calendar calendar = Wotd.getTodayUTC();
            Calendar calendarDisplay = Wotd.getTodayUTC();
            calendarDisplay.setTimeZone(TimeZone.getDefault());
            for (int i = 0; i < 100; i++) {
                Random random = new Random();
                random.setSeed(calendar.getTimeInMillis());
                String date = DateUtils.formatDateTime(getContext(),
                        calendarDisplay.getTimeInMillis(),
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);
                int position = random.nextInt(cursor.getCount());
                if (cursor.moveToPosition(position)) {
                    String word = cursor.getString(0);
                    @ColorRes int color = (i % 2 == 0) ? R.color.row_background_color_even : R.color.row_background_color_odd;
                    data.add(new WotdEntry(
                            word,
                            date,
                            ContextCompat.getColor(getContext(), color),
                            favorites.contains(word)));
                }
                calendar.add(Calendar.DAY_OF_YEAR, -1);
                calendarDisplay.add(Calendar.DAY_OF_YEAR, -1);

            }

        } finally {
            cursor.close();
        }
        return new ResultListData<>(getContext().getString(R.string.wotd_list_header), false, data);
    }

    private ResultListData<WotdEntry> emptyResult() {
        return new ResultListData<>(getContext().getString(R.string.wotd_list_header), false, new ArrayList<>());
    }

}
