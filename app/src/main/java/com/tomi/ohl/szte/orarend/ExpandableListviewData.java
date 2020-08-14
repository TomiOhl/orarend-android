package com.tomi.ohl.szte.orarend;

import android.content.Context;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

class ExpandableListviewData {
    static LinkedHashMap<String, List<String>> getData(Context ctx) {

        LinkedHashMap<String, List<String>> napok = new LinkedHashMap<>();
        List<String> Monday = new ArrayList<>();
        List<String> Tuesday = new ArrayList<>();
        List<String> Wednesday = new ArrayList<>();
        List<String> Thursday = new ArrayList<>();
        List<String> Friday = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(ctx.getAssets().open("timetable.txt")))) {
            String line;
            String day = "monday";
            while ((line = reader.readLine()) != null) {
                if (line.equals("monday") || line.equals("tuesday") || line.equals("wednesday") || line.equals("thursday") || line.equals("friday")) {
                    day = line;
                    continue;
                }
                switch(day) {
                    case "monday": Monday.add(line); break;
                    case "tuesday": Tuesday.add(line); break;
                    case "wednesday": Wednesday.add(line); break;
                    case "thursday": Thursday.add(line); break;
                    default: Friday.add(line);
                }
            }
        } catch (Exception e) {
            Toast.makeText(ctx, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }

        napok.put(ctx.getResources().getString(R.string.monday), Monday);
        napok.put(ctx.getResources().getString(R.string.tuesday), Tuesday);
        napok.put(ctx.getResources().getString(R.string.wednesday), Wednesday);
        napok.put(ctx.getResources().getString(R.string.thursday), Thursday);
        //napok.put(ctx.getResources().getString(R.string.friday), Friday);

        return napok;
    }
}