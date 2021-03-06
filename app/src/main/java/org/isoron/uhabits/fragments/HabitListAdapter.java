/*
 * Copyright (C) 2016 Álinson Santos Xavier <isoron@gmail.com>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.isoron.uhabits.fragments;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.isoron.helpers.DateHelper;
import org.isoron.uhabits.R;
import org.isoron.uhabits.helpers.ListHabitsHelper;
import org.isoron.uhabits.loaders.HabitListLoader;
import org.isoron.uhabits.models.Habit;

import java.util.List;

class HabitListAdapter extends BaseAdapter
{
    private LayoutInflater inflater;
    private HabitListLoader loader;
    private ListHabitsHelper helper;
    private List selectedPositions;
    private View.OnLongClickListener onCheckmarkLongClickListener;
    private View.OnClickListener onCheckmarkClickListener;

    public HabitListAdapter(Context context, HabitListLoader loader)
    {
        this.loader = loader;

        inflater = LayoutInflater.from(context);
        helper = new ListHabitsHelper(context, loader);
    }

    @Override
    public int getCount()
    {
        return loader.habits.size();
    }

    @Override
    public Habit getItem(int position)
    {
        return loader.habitsList.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return (getItem(position)).getId();
    }

    @Override
    public View getView(int position, View view, ViewGroup parent)
    {
        final Habit habit = loader.habitsList.get(position);

        if (view == null || (Long) view.getTag(R.id.timestamp_key) != DateHelper.getStartOfToday())
        {
            view = inflater.inflate(R.layout.list_habits_item, null);
            helper.initializeLabelAndIcon(view);
            helper.inflateCheckmarkButtons(view, onCheckmarkLongClickListener,
                    onCheckmarkClickListener, inflater);
        }

        TextView tvStar = ((TextView) view.findViewById(R.id.tvStar));
        TextView tvName = (TextView) view.findViewById(R.id.label);
        LinearLayout llInner = (LinearLayout) view.findViewById(R.id.llInner);
        LinearLayout llButtons = (LinearLayout) view.findViewById(R.id.llButtons);

        llInner.setTag(R.string.habit_key, habit.getId());

        helper.updateNameAndIcon(habit, tvStar, tvName);
        helper.updateCheckmarkButtons(habit, llButtons);

        boolean selected = selectedPositions.contains(position);
        helper.updateHabitBackground(llInner, selected);

        return view;
    }

    public void setSelectedPositions(List selectedPositions)
    {
        this.selectedPositions = selectedPositions;
    }

    public void setOnCheckmarkLongClickListener(View.OnLongClickListener listener)
    {
        this.onCheckmarkLongClickListener = listener;
    }

    public void setOnCheckmarkClickListener(View.OnClickListener listener)
    {
        this.onCheckmarkClickListener = listener;
    }
}
