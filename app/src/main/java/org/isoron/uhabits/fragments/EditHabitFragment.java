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

import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.colorpicker.ColorPickerDialog;
import com.android.colorpicker.ColorPickerSwatch;
import com.android.datetimepicker.time.RadialPickerLayout;
import com.android.datetimepicker.time.TimePickerDialog;

import org.isoron.helpers.ColorHelper;
import org.isoron.helpers.DateHelper;
import org.isoron.helpers.DialogHelper.OnSavedListener;
import org.isoron.uhabits.R;
import org.isoron.uhabits.commands.Command;
import org.isoron.uhabits.commands.CreateHabitCommand;
import org.isoron.uhabits.commands.EditHabitCommand;
import org.isoron.uhabits.dialogs.WeekdayPickerDialog;
import org.isoron.uhabits.models.Habit;

import java.util.Arrays;
import java.util.Date;

public class EditHabitFragment extends DialogFragment
        implements OnClickListener, WeekdayPickerDialog.OnWeekdaysPickedListener,
        TimePickerDialog.OnTimeSetListener
{
    private Integer mode;
    static final int EDIT_MODE = 0;
    static final int CREATE_MODE = 1;

    private OnSavedListener onSavedListener;

    private Habit originalHabit;
    private Habit modifiedHabit;

    private TextView tvName;
    private TextView tvDescription;
    private TextView tvFreqNum;
    private TextView tvFreqDen;
    private TextView tvReminderTime;
    private TextView tvReminderDays;

    private SharedPreferences prefs;
    private boolean is24HourMode;

    public static EditHabitFragment editSingleHabitFragment(long id)
    {
        EditHabitFragment frag = new EditHabitFragment();
        Bundle args = new Bundle();
        args.putLong("habitId", id);
        args.putInt("editMode", EDIT_MODE);
        frag.setArguments(args);
        return frag;
    }

    public static EditHabitFragment createHabitFragment()
    {
        EditHabitFragment frag = new EditHabitFragment();
        Bundle args = new Bundle();
        args.putInt("editMode", CREATE_MODE);
        frag.setArguments(args);
        return frag;
    }

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.edit_habit, container, false);
        tvName = (TextView) view.findViewById(R.id.input_name);
        tvDescription = (TextView) view.findViewById(R.id.input_description);
        tvFreqNum = (TextView) view.findViewById(R.id.input_freq_num);
        tvFreqDen = (TextView) view.findViewById(R.id.input_freq_den);
        tvReminderTime = (TextView) view.findViewById(R.id.inputReminderTime);
        tvReminderDays = (TextView) view.findViewById(R.id.inputReminderDays);

        Button buttonSave = (Button) view.findViewById(R.id.buttonSave);
        Button buttonDiscard = (Button) view.findViewById(R.id.buttonDiscard);
        ImageButton buttonPickColor = (ImageButton) view.findViewById(R.id.buttonPickColor);

        buttonSave.setOnClickListener(this);
        buttonDiscard.setOnClickListener(this);
        tvReminderTime.setOnClickListener(this);
        tvReminderDays.setOnClickListener(this);
        buttonPickColor.setOnClickListener(this);

        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        Bundle args = getArguments();
        mode = (Integer) args.get("editMode");

        is24HourMode = DateFormat.is24HourFormat(getActivity());

        if (mode == CREATE_MODE)
        {
            getDialog().setTitle(R.string.create_habit);
            modifiedHabit = new Habit();

            int defaultNum = prefs.getInt("pref_default_habit_freq_num", modifiedHabit.freqNum);
            int defaultDen = prefs.getInt("pref_default_habit_freq_den", modifiedHabit.freqDen);
            int defaultColor = prefs.getInt("pref_default_habit_color", modifiedHabit.color);

            modifiedHabit.color = defaultColor;
            modifiedHabit.freqNum = defaultNum;
            modifiedHabit.freqDen = defaultDen;
        }
        else if (mode == EDIT_MODE)
        {
            Long habitId = (Long) args.get("habitId");
            if(habitId == null) throw new IllegalArgumentException("habitId must be specified");

            originalHabit = Habit.get(habitId);
            modifiedHabit = new Habit(originalHabit);

            getDialog().setTitle(R.string.edit_habit);
            tvName.append(modifiedHabit.name);
            tvDescription.append(modifiedHabit.description);
        }

        if(savedInstanceState != null)
        {
            modifiedHabit.color = savedInstanceState.getInt("color", modifiedHabit.color);
            modifiedHabit.reminderMin = savedInstanceState.getInt("reminderMin", -1);
            modifiedHabit.reminderHour = savedInstanceState.getInt("reminderHour", -1);
            modifiedHabit.reminderDays = savedInstanceState.getInt("reminderDays", -1);

            if(modifiedHabit.reminderMin < 0)
            {
                modifiedHabit.reminderMin = null;
                modifiedHabit.reminderHour = null;
                modifiedHabit.reminderDays = 127;
            }
        }

        tvFreqNum.append(modifiedHabit.freqNum.toString());
        tvFreqDen.append(modifiedHabit.freqDen.toString());

        changeColor(modifiedHabit.color);
        updateReminder();

        return view;
    }

    private void changeColor(Integer color)
    {
        modifiedHabit.color = color;
        tvName.setTextColor(color);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("pref_default_habit_color", color);
        editor.apply();
    }

    @SuppressWarnings("ConstantConditions")
    private void updateReminder()
    {
        if (modifiedHabit.hasReminder())
        {
            tvReminderTime.setTextColor(Color.BLACK);
            tvReminderTime.setText(DateHelper.formatTime(getActivity(), modifiedHabit.reminderHour,
                    modifiedHabit.reminderMin));
            tvReminderDays.setVisibility(View.VISIBLE);

            boolean weekdays[] = DateHelper.unpackWeekdayList(modifiedHabit.reminderDays);
            tvReminderDays.setText(DateHelper.formatWeekdayList(getActivity(), weekdays));
        }
        else
        {
            tvReminderTime.setTextColor(Color.GRAY);
            tvReminderTime.setText(R.string.reminder_off);
            tvReminderDays.setVisibility(View.GONE);
        }
    }

    public void setOnSavedListener(OnSavedListener onSavedListener)
    {
        this.onSavedListener = onSavedListener;
    }

	@Override
    public void onClick(View v)
    {
        switch(v.getId())
        {
            case R.id.inputReminderTime:
                onDateSpinnerClick();
                break;

            case R.id.inputReminderDays:
                onWeekdayClick();
                break;

            case R.id.buttonSave:
                onSaveButtonClick();
                break;

            case R.id.buttonDiscard:
                dismiss();
                break;

            case R.id.buttonPickColor:
                onColorButtonClick();
                break;
        }
    }

    private void onColorButtonClick()
    {
        ColorPickerDialog picker = ColorPickerDialog.newInstance(
                R.string.color_picker_default_title, ColorHelper.palette, modifiedHabit.color, 4,
                ColorPickerDialog.SIZE_SMALL);

        picker.setOnColorSelectedListener(new ColorPickerSwatch.OnColorSelectedListener()
        {
            public void onColorSelected(int color)
            {
                changeColor(color);
            }
        });
        picker.show(getFragmentManager(), "picker");
    }

    private void onSaveButtonClick()
    {
        modifiedHabit.name = tvName.getText().toString().trim();
        modifiedHabit.description = tvDescription.getText().toString().trim();
        modifiedHabit.freqNum = Integer.parseInt(tvFreqNum.getText().toString());
        modifiedHabit.freqDen = Integer.parseInt(tvFreqDen.getText().toString());

        if (!validate()) return;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("pref_default_habit_freq_num", modifiedHabit.freqNum);
        editor.putInt("pref_default_habit_freq_den", modifiedHabit.freqDen);
        editor.apply();

        Command command = null;
        Habit savedHabit = null;

        if (mode == EDIT_MODE)
        {
            command = new EditHabitCommand(originalHabit, modifiedHabit);
            savedHabit = originalHabit;
        }
        else if (mode == CREATE_MODE)
        {
            command = new CreateHabitCommand(modifiedHabit);
        }

        if (onSavedListener != null) onSavedListener.onSaved(command, savedHabit);

        dismiss();
    }

    private boolean validate()
    {
        Boolean valid = true;

        if (modifiedHabit.name.length() == 0)
        {
            tvName.setError(getString(R.string.validation_name_should_not_be_blank));
            valid = false;
        }

        if (modifiedHabit.freqNum <= 0)
        {
            tvFreqNum.setError(getString(R.string.validation_number_should_be_positive));
            valid = false;
        }

        if (modifiedHabit.freqNum > modifiedHabit.freqDen)
        {
            tvFreqNum.setError(getString(R.string.validation_at_most_one_rep_per_day));
            valid = false;
        }

        return valid;
    }

    @SuppressWarnings("ConstantConditions")
    private void onDateSpinnerClick()
    {
        int defaultHour = 8;
        int defaultMin = 0;

        if (modifiedHabit.hasReminder())
        {
            defaultHour = modifiedHabit.reminderHour;
            defaultMin = modifiedHabit.reminderMin;
        }

        TimePickerDialog timePicker =
                TimePickerDialog.newInstance(this, defaultHour, defaultMin, is24HourMode);
        timePicker.show(getFragmentManager(), "timePicker");
    }

    @SuppressWarnings("ConstantConditions")
    private void onWeekdayClick()
    {
        if(!modifiedHabit.hasReminder()) return;

        WeekdayPickerDialog dialog = new WeekdayPickerDialog();
        dialog.setListener(this);
        dialog.setSelectedDays(DateHelper.unpackWeekdayList(modifiedHabit.reminderDays));
        dialog.show(getFragmentManager(), "weekdayPicker");
    }

    @Override
    public void onTimeSet(RadialPickerLayout view, int hour, int minute)
    {
        modifiedHabit.reminderHour = hour;
        modifiedHabit.reminderMin = minute;
        modifiedHabit.reminderDays = DateHelper.ALL_WEEK_DAYS;
        updateReminder();
    }

    @Override
    public void onTimeCleared(RadialPickerLayout view)
    {
        modifiedHabit.clearReminder();
        updateReminder();
    }

    @Override
    public void onWeekdaysPicked(boolean[] selectedDays)
    {
        int count = 0;
        for(int i = 0; i < 7; i++)
            if(selectedDays[i]) count++;

        if(count == 0) Arrays.fill(selectedDays, true);

        modifiedHabit.reminderDays = DateHelper.packWeekdayList(selectedDays);
        updateReminder();
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putInt("color", modifiedHabit.color);

        if(modifiedHabit.hasReminder())
        {
            outState.putInt("reminderMin", modifiedHabit.reminderMin);
            outState.putInt("reminderHour", modifiedHabit.reminderHour);
            outState.putInt("reminderDays", modifiedHabit.reminderDays);
        }
    }
}
