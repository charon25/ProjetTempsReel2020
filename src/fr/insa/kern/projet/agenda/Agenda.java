package fr.insa.kern.projet.agenda;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;

// Classe qui permet l'interface avec la classe Calendar pour gérer un calendrier avec des créneaux
public class Agenda { // Créneaux de deux heures, de 8h à 18h tous les jours de la semaine

    public Day[] days; // Liste de jours
    private Calendar date; // Date de création de l'agenda

    // Caractérise un créneau (disponible, déjà passé, trop loin dans le futur, déjà réservé)
    public enum Availability {AVAILABLE, PAST, TOO_FAR, ALREADY_BOOKED}

    // Renvoie l'année du prochain couple mois/jour (mois entre 0 et 11). Par exemple, si on est le 24 novembre 2020,
    // alors 'année du 28 novembre sera également 2020, mais celle du 20 novembre sera 2021
    public static String getYearOfDayMonth(int day, int month) {
        Calendar currentDate = Calendar.getInstance(); // Date actuelle
        if (month < currentDate.get(Calendar.MONTH)) { // Si le mois est antérieur, alors on est l'année d'après
            return Integer.toString(currentDate.get(Calendar.YEAR) + 1);
        } else if (month > currentDate.get(Calendar.MONTH)) { // Si le mois est postérieur, on est la même année
            return Integer.toString(currentDate.get(Calendar.YEAR));
        } else { // Si les mois sont égaux, on discrimine de façon indentique sur le jour
            if (day <= currentDate.get(Calendar.DATE)) {
                return Integer.toString(currentDate.get(Calendar.YEAR) + 1);
            } else {
                return Integer.toString(currentDate.get(Calendar.YEAR));
            }
        }
    }

    // Constructeur avec un nombre de jour après le jour actuel
    public Agenda(int daysCount) {
        this.days = new Day[daysCount];
        this.date = Calendar.getInstance(); // Date actuelle, en remettant heure, minutes, secondes et millisecondes à 0
        this.date.set(Calendar.HOUR, 0);
        this.date.set(Calendar.MINUTE, 0);
        this.date.set(Calendar.SECOND, 0);
        this.date.set(Calendar.MILLISECOND, 0);
        for (int i = 1; i <= daysCount; i++) { // On ajoute 1 à la date à chaque tour de boucle et on crée un jour avec cette date
            this.date.add(Calendar.DATE, 1);
            this.days[i - 1] = new Day(this.date);
        }
        this.date.add(Calendar.DATE, -daysCount); // On réinitialise la date à la valeur d'aujourd'hui
    }

    // Constructeur avec un fichier donné par son chemin
    // Le format est : aaaa-mm-jj[:h/<user>/<date de réservation>, ...]
    public Agenda(String path) {
        try {
            // On lit le fichier
            FileReader reader = new FileReader(path);
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line;
            ArrayList<Day> days = new ArrayList<>();
            // Pour chaque ligne on ajoute un nouveau jour
            while ((line = bufferedReader.readLine()) != null) {
                days.add(new Day(line));
            }
            this.days = new Day[days.size()];
            days.toArray(this.days); // On convertir l'ArrayList en array simple
            this.date = Calendar.getInstance();
            this.date.set(Calendar.HOUR, 0);
            this.date.set(Calendar.MINUTE, 0);
            this.date.set(Calendar.SECOND, 0);
            this.date.set(Calendar.MILLISECOND, 0); // On prends la date actuelle
            reader.close();
        } catch (IOException ex) {}
    }

    // Réserve un créneau. Renvoie true si la réservation a fonctionnée, false sinon
    // user : utilisateur qui fait la réservation, dateString : jour au format aaaa-mm-jj, hour : heure de la réservation
    // monthStartsAtZero : indique si le mois est compris entre 0 et 11 ou 1 et 12
    public boolean bookSlot(String user, String dateString, int hour, boolean monthStartsAtZero) { // Date au format yyyy-mm-dd (aaaa-mm-jj)
        // On calcule la date donnée par la variable dateString
        Calendar date = Calendar.getInstance();
        String[] args = dateString.split("-");
        date.set(Calendar.YEAR, Integer.parseInt(args[0]));
        date.set(Calendar.MONTH, Integer.parseInt(args[1]) - (monthStartsAtZero ? 0 : 1));
        date.set(Calendar.DATE, Integer.parseInt(args[2]));
        date.set(Calendar.HOUR, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        synchronized (this.days) { //On synchronise pour éviter d'avoir des problèmes si plusieurs threads essaye d'y accéder en même temps
            // On calcule dans combien de temps est cette date par rapport au premier jour de l'agenda
            int daysIndex = DateDiffInDays(date, days[0].getDate());
            if (daysIndex <= 0 || daysIndex > this.days.length) { // Si elle est dans le passé ou trop loin dans le futur on s'arrête
                return false;
            }

            // Sinon on essaye de réserver
            return this.days[daysIndex].bookSlot(user, hour);
        }
    }

    // Renvoie l'ensemble des créneaux réservés
    public ArrayList<Slot> getAllBookedSlots() {
        ArrayList<Slot> slots = new ArrayList<>();
        for (Day day : days) { // On parcourt tous les jours
            for (int j = 0; j < day.getSlots().length; j++) { // On parcourt tous les créneaux de chaque jour
                if (day.getSlots()[j] != null) { // Si le créneau a été réservé
                    slots.add(day.getSlots()[j]);
                }
            }
        }
        return slots;
    }

    // Indique si un créneau donné est disponible, et si non, pourquoi
    // dateString : jour au format aaaa-mm-jj, hour : heure
    // monthStartsAtZero : indique si les mois sont compris entre 0 et 11 ou 1 et 12
    public Availability isSlotAvailable(String dateString, int hour, boolean monthStartsAtZero) {
        Calendar date = Calendar.getInstance(); // On calcule la date
        String[] args = dateString.split("-");
        date.set(Calendar.YEAR, Integer.parseInt(args[0]));
        date.set(Calendar.MONTH, Integer.parseInt(args[1]) - (monthStartsAtZero ? 0 : 1));
        date.set(Calendar.DATE, Integer.parseInt(args[2]));
        date.set(Calendar.HOUR, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        // On calcule dans combien de temps elle est par rapport au premier jour de l'agenda
        int daysIndex = DateDiffInDays(date, days[0].getDate());

        if (daysIndex <= 0) { // Si elle est déjà passée
            return Availability.PAST;
        } else if (daysIndex > this.days.length) { // Si elle est trop loin dans le futur
            return Availability.TOO_FAR;
        } else if (this.days[daysIndex].getSlotByHour(hour) != null) { // Si le créneau est déjà réservé
            return Availability.ALREADY_BOOKED;
        }
        // Sinon le créneau est disponible
        return Availability.AVAILABLE;
    }

    // Enregistre l'agenda au chemin fourni
    public void save(String path) {
        String output = ""; // String qui contiendra la sortie
        for (Day day : days) {
            output += day.toString() + "\n"; // On ajoute chaque jour
        }
        try { // On l'écrit dans le chemin adapté
            FileWriter writer = new FileWriter(path);
            writer.write(output.trim());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Renvoie le nombre de jour entre deux date, en calculte l'écart en millisecondes, et en divisant par
    // 86400000 = 24 * 60 * 60 * 1000
    private static int DateDiffInDays(Calendar date1, Calendar date2) {
        return (int) Math.floor((date1.getTimeInMillis() - date2.getTimeInMillis()) / 86400000);
    }

    // Renvoie le dernier jour du tableau sous le format "dd <mois en toute lettre> aaaa"
    public String getLastAvailableDay() {
        return days[days.length - 1].getDateStringDaysFirst();
    }



}
