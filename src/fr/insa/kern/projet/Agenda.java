package fr.insa.kern.projet;

import java.io.*;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class Agenda { //Créneaux de deux heures, de 8h à 18h, 5 jours par semaine

    private Day[] days;
    private Calendar date;

    public enum Availability {AVAILABLE, PAST, TOO_FAR, ALREADY_BOOKED}

    //mois entre 0 et 11
    public static String getYearOfDayMonth(int day, int month) {
        Calendar currentDate = Calendar.getInstance();
        if (month < currentDate.get(Calendar.MONTH)) {
            return Integer.toString(currentDate.get(Calendar.YEAR) + 1);
        } else if (month > currentDate.get(Calendar.MONTH)) {
            return Integer.toString(currentDate.get(Calendar.YEAR));
        } else {
            if (day < currentDate.get(Calendar.DATE)) {
                return Integer.toString(currentDate.get(Calendar.YEAR) + 1);
            } else {
                return Integer.toString(currentDate.get(Calendar.YEAR));
            }
        }
    }

    public Agenda(int daysCount) {
        this.days = new Day[daysCount];
        this.date = Calendar.getInstance();
        this.date.set(Calendar.HOUR, 0);
        this.date.set(Calendar.MINUTE, 0);
        this.date.set(Calendar.SECOND, 0);
        this.date.set(Calendar.MILLISECOND, 0);
        for (int i = 1; i <= daysCount; i++) {
            this.date.add(Calendar.DATE, 1);
            this.days[i - 1] = new Day(this.date);
        }
        this.date.add(Calendar.DATE, -daysCount);
    }

    public Agenda(String path) {
        try {
            FileReader reader = new FileReader(path);
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line;
            ArrayList<Day> days = new ArrayList<Day>();
            while ((line = bufferedReader.readLine()) != null) {
                days.add(new Day(line));
            }
            this.days = new Day[days.size()];
            days.toArray(this.days);
            this.date = Calendar.getInstance();
            this.date.set(Calendar.HOUR, 0);
            this.date.set(Calendar.MINUTE, 0);
            this.date.set(Calendar.SECOND, 0);
            this.date.set(Calendar.MILLISECOND, 0);
            reader.close();
        } catch (IOException ex) {

        }
    }

    public boolean bookSlot(String user, String dateString, int hour, boolean monthStartsAtZero) { //Date au format yyyy-mm-dd (aaaa-mm-jj)
        Calendar date = Calendar.getInstance();
        String[] args = dateString.split("-");
        date.set(Calendar.YEAR, Integer.parseInt(args[0]));
        date.set(Calendar.MONTH, Integer.parseInt(args[1]) - (monthStartsAtZero ? 0 : 1));
        date.set(Calendar.DATE, Integer.parseInt(args[2]));
        date.set(Calendar.HOUR, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        int daysIndex = DateDiffInDays(date, this.date);
        if (daysIndex > this.days.length) {
            return false;
        }
        return this.days[daysIndex - 1].bookSlot(user, hour);
    }

    public ArrayList<Slot> getSlotsByUser(String user) {
        ArrayList<Slot> slots = new ArrayList<>();
        for (int i = 0; i < days.length; i++) {
            for (int j = 0; j < days[i].getSlots().length; j++) {
                if (days[i].getSlots()[j] != null && days[i].getSlots()[j].getUser().equals(user)) {
                    slots.add(days[i].getSlots()[j]);
                }
            }
        }
        return slots;
    }

    public ArrayList<Slot> getAllSlots() {
        ArrayList<Slot> slots = new ArrayList<>();
        for (int i = 0; i < days.length; i++) {
            for (int j = 0; j < days[i].getSlots().length; j++) {
                if (days[i].getSlots()[j] != null) {
                    slots.add(days[i].getSlots()[j]);
                }
            }
        }
        return slots;
    }

    public Availability isSlotAvailable(String dateString, int hour, boolean monthStartsAtZero) {
        System.out.println("is slot avaib : " + dateString);
        Calendar date = Calendar.getInstance();
        String[] args = dateString.split("-");
        date.set(Calendar.YEAR, Integer.parseInt(args[0]));
        date.set(Calendar.MONTH, Integer.parseInt(args[1]) - (monthStartsAtZero ? 0 : 1));
        date.set(Calendar.DATE, Integer.parseInt(args[2]));
        date.set(Calendar.HOUR, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        System.out.println(date.get(Calendar.YEAR) + "/" + date.get(Calendar.MONTH) + "/" + date.get(Calendar.DATE));
        int daysIndex = DateDiffInDays(date, this.date);
        System.out.println(daysIndex);
        if (daysIndex <= 0) {
            return Availability.PAST;
        } else if (daysIndex > this.days.length) {
            return Availability.TOO_FAR;
        } else if (this.days[daysIndex - 1].getSlotByHour(hour) != null) {
            return Availability.ALREADY_BOOKED;
        }
        return Availability.AVAILABLE;
    }

    public void save(String path) {
        String output = "";
        for (int i = 0; i < days.length; i++) {
            output += days[i].toString() + "\n";
        }
        try {
            FileWriter writer = new FileWriter(path);
            writer.write(output.trim());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int DateDiffInDays(Calendar date1, Calendar date2) {
        return (int) Math.floor((date1.getTimeInMillis() - date2.getTimeInMillis()) / 86400000);
    }

    public class Day {

        private final int SLOT_COUNT = 5;
        private Calendar date;
        private Slot[] slots;

        public Day(Calendar dayDate) {
            this.date = Calendar.getInstance();
            this.date.setTimeInMillis(dayDate.getTimeInMillis());
            this.slots = new Slot[SLOT_COUNT];
        }

        public Day(String dayString) {
            try {
                this.date = Calendar.getInstance();
                String[] args = dayString.split(":")[0].split("-");
                this.date.set(Calendar.YEAR, Integer.parseInt(args[0]));
                this.date.set(Calendar.MONTH, Integer.parseInt(args[1]));
                this.date.set(Calendar.DATE, Integer.parseInt(args[2]));
                this.date.set(Calendar.HOUR, 0);
                this.date.set(Calendar.MINUTE, 0);
                this.date.set(Calendar.SECOND, 0);
                this.date.set(Calendar.MILLISECOND, 0);

                this.slots = new Slot[SLOT_COUNT];
                String[] loadedSlots = dayString.split(":")[1].split(",");
                for (int i = 0; i < loadedSlots.length; i++) {
                    args = loadedSlots[i].split("/");
                    this.slots[Integer.parseInt(args[0])] = new Slot(args[1], Integer.parseInt(args[0]), Long.parseLong(args[2]), this.date);
                }
            } catch (ArrayIndexOutOfBoundsException ex) {

            }
        }

        public boolean bookSlot(String user, int hour) {
            hour = getIndexFromHour(hour);
            if (slots[hour] == null) {
                slots[hour] = new Slot(user, date, hour);
                return true;
            } else {
                return false;
            }
        }

        private int getIndexFromHour(int hour) {
            return (int) ((hour - 8) / 2);
        }

        public Calendar getDate() {
            return date;
        }

        public String getDateString() {
            return date.get(Calendar.YEAR) + "-" + date.get(Calendar.MONTH) + "-" + date.get(Calendar.DATE);
        }

        public Slot[] getSlots() {
            return slots;
        }

        public Slot getSlotByHour(int hour) {
            return slots[getIndexFromHour(hour)];
        }

        public String toString() {
            String output = getDateString() + ":";
            for (int i = 0; i < SLOT_COUNT; i++) {
                if (slots[i] != null) {
                    output += slots[i].toString() + ",";
                }
            }
            return output.substring(0, output.length() - 1);
        }
    }

    public class Slot {

        private String user;
        private Calendar day;
        private Calendar reservationDate;
        private int hour;

        public Slot(String user, Calendar day, int hour) {
            this.hour = hour;
            this.user = user;
            this.day = day;
            this.reservationDate = Calendar.getInstance();
        }

        public Slot(String user, int hour, long reservationTime, Calendar day) {
            this.hour = hour;
            this.user = user;
            this.day = day;
            this.reservationDate = Calendar.getInstance();
            this.reservationDate.setTimeInMillis(reservationTime);
        }

        public String getUser() {
            return user;
        }

        public Calendar getReservationDate() {
            return reservationDate;
        }

        public String getReservationString() {
            return padLeft(Integer.toString(getHour()), 2) + "h-" + padLeft(Integer.toString(getHour() + 2), 2) + "h le " + day.get(Calendar.DATE) + " " + Classifier.MONTHS[day.get(Calendar.MONTH)];
        }

        public int getHour() {
            return 2 * hour + 8;
        }

        public String toString() {
            return String.valueOf(hour) + "/" + user + "/" + reservationDate.getTimeInMillis(); //TODO
        }

        private String padLeft(String str, int length) {
            if (str.length() >= length) {
                return str;
            } else {
                for (int i = length; i > str.length(); i--) {
                    str = "0" + str;
                }
            }
            return str;
        }
    }

}
