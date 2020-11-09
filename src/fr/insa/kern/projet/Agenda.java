package fr.insa.kern.projet;

import java.io.*;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class Agenda { //Créneaux de deux heures, de 8h à 18h, 5 jours par semaine

    private Day[] days;
    private Calendar date;

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
            reader.close();
        } catch (IOException ex) {

        }
    }

    public boolean bookSlot(String user, String dateString, int hour) { //Date au format yyyy-mm-dd (aaaa-mm-jj)
        Calendar date = Calendar.getInstance();
        String[] args = dateString.split("-");
        date.set(Calendar.YEAR, Integer.parseInt(args[0]));
        date.set(Calendar.MONTH, Integer.parseInt(args[1]) - 1);
        date.set(Calendar.DATE, Integer.parseInt(args[2]));
        date.set(Calendar.HOUR, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        int daysIndex = DateDiffInDays(this.date, date);
        if (daysIndex > this.days.length) {
            return false;
        }
        return this.days[daysIndex].bookSlot(user, hour);
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
        return (int) Math.floor(Math.abs(date1.getTimeInMillis() - date2.getTimeInMillis()) / 86400000);
    }

    public class Day {

        private final int SLOT_COUNT = 5;
        private Calendar date;
        private Slot[] slots;

        public Day(Calendar date) {
            this.date = Calendar.getInstance();
            this.date.setTimeInMillis(date.getTimeInMillis());
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
                    this.slots[Integer.parseInt(args[0])] = new Slot(args[1], Integer.parseInt(args[0]), Long.parseLong(args[2]));
                }
            } catch (ArrayIndexOutOfBoundsException ex) {

            }
        }

        public boolean bookSlot(String user, int hour) {
            hour = getIndexFromHour(hour);
            if (slots[hour] == null) {
                slots[hour] = new Slot(user, hour);
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
        private Calendar reservationDate;
        private int hour;

        public Slot(String user, int hour) {
            this.hour = hour;
            this.user = user;
            this.reservationDate = Calendar.getInstance();
        }
        public Slot(String user, int hour, long reservationTime) {
            this.hour = hour;
            this.user = user;
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
            return reservationDate.get(Calendar.YEAR) + "-" + reservationDate.get(Calendar.MONTH) + "-" + reservationDate.get(Calendar.DATE) + "-" + reservationDate.get(Calendar.HOUR) + "-" + reservationDate.get(Calendar.MINUTE) + "-" + reservationDate.get(Calendar.SECOND) ;
        }
        public int getHour() {
            return 2 * hour + 8;
        }
        public String toString() {
            return String.valueOf(hour) + "/" + user + "/" + reservationDate.getTimeInMillis(); //TODO
        }
    }

    public static void main(String[] args) {
        Calendar date = Calendar.getInstance();
        System.out.println(date.getTime());
        Agenda ag = new Agenda(30);
        System.out.println(ag.bookSlot("charon", "2020-11-27", 8));
        System.out.println(ag.bookSlot("charon", "2020-11-27", 10));
        System.out.println(ag.bookSlot("charon2", "2020-11-27", 8));
        System.out.println(ag.bookSlot("charon3", "2020-11-28", 8));
        System.out.println(ag.bookSlot("charon2", "2021-11-28", 8));
        ArrayList<Slot> slots = ag.getSlotsByUser("charon");
        for (int i = 0; i < slots.size(); i++) {
            System.out.println(slots.get(i).getHour());
        }
        ag.save("D:\\test\\agenda.txt");

        Agenda ag2 = new Agenda("D:\\test\\agenda.txt");
        slots = ag2.getSlotsByUser("charon");
        for (int i = 0; i < slots.size(); i++) {
            System.out.println(slots.get(i).getHour());
        }

    }

}
