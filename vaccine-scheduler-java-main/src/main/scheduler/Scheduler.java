package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;
    private static int numAppointments = 1;

    public static void main(String[] args) throws SQLException {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) throws SQLException {
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username is already taken
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        // check 3: check if the password is strong
        if (!strongPassword(password)) {
            System.out.println("Password is not strong, try again!");
            System.out.println("It should include:");
            System.out.println("•At least 8 characters");
            System.out.println("•A mixture of both uppercase and lowercase letters");
            System.out.println("•A mixture of letters and numbers");
            System.out.println("•Inclusion of at least one special character, from “!”, “@”, “#”, “?”");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the patient
        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to patient information to our database
            currentPatient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void createCaregiver(String[] tokens) throws SQLException {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        // check 3: check if the password is strong (extra credit)
        if (!strongPassword(password)) {
            System.out.println("Password is not strong, try again!");
            System.out.println("It should include:");
            System.out.println("•At least 8 characters");
            System.out.println("•A mixture of both uppercase and lowercase letters");
            System.out.println("•A mixture of letters and numbers");
            System.out.println("•Inclusion of at least one special character, from “!”, “@”, “#”, “?”");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean strongPassword(String password) {
        // at least 8 characters
        if (password.length() < 8) {
            return false;
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasNumber = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) { // uppercase letters
                hasUpper = true;
            } else if (Character.isLowerCase(c)) { // lowercase letters
                hasLower = true;
            } else if (Character.isDigit(c)) { // numbers
                hasNumber = true;
            } else if (c == '!' || c == '@' || c == '#' || c == '?') { // special characters
                hasSpecial = true;
            } else {
                return false;
            }
        }
        return hasUpper && hasLower && hasNumber && hasSpecial;
    }

    private static void loginPatient(String[] tokens) {
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) throws SQLException {
        // search_caregiver_schedule <date>
        // check 1: the user must be logged in first
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }

        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }

        String date = tokens[1];
        Date d = Date.valueOf(date);

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {
            PreparedStatement getAvailableCaregiver =
                    con.prepareStatement("SELECT Username FROM Availabilities WHERE Time = ? ORDER BY Username");
            getAvailableCaregiver.setDate(1, d);
            ResultSet rs1 = getAvailableCaregiver.executeQuery();
            while (rs1.next()) {
                System.out.println("Available caregiver: " + rs1.getString(1));
            }

            PreparedStatement getVaccines = con.prepareStatement("SELECT * FROM vaccines");
            ResultSet rs2 = getVaccines.executeQuery();
            while (rs2.next()) {
                System.out.println("Vaccine: " + rs2.getString(1) +
                        ", Available Doses: " + rs2.getInt(2));
            }
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) throws SQLException {
        // reserve <date> <vaccine>
        if (currentPatient == null) {
            // check 1: if the user is logged in
            if (currentCaregiver == null) {
                System.out.println("Please login first!");
            } else { // check 2: if the current user is a patient
                System.out.println("Please login as a patient!");
            }
            return;
        }

        // check 3: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }

        String date = tokens[1];
        String vaccineName = tokens[2];
        Vaccine vaccine;

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {
            Date d = Date.valueOf(date);
            PreparedStatement getAvailableCaregiver =
                    con.prepareStatement("SELECT Username FROM Availabilities WHERE Time = ? ORDER BY Username");
            getAvailableCaregiver.setDate(1, d);
            ResultSet rs1 = getAvailableCaregiver.executeQuery();
            String availableCaregiver;
            if (rs1.next()) {
                availableCaregiver = rs1.getString(1);
            } else {
                System.out.println("No Caregiver is available!");
                return;
            }

            PreparedStatement getAvailableVaccine =
                    con.prepareStatement("SELECT Name,Doses FROM Vaccines WHERE Name = ? AND Doses > 0");
            getAvailableVaccine.setString(1, vaccineName);
            ResultSet rs2 = getAvailableVaccine.executeQuery();
            if (!rs2.next()) {
                System.out.println("Not enough available doses!");
                return;
            } else {
                try {
                    vaccine = new Vaccine.VaccineGetter(vaccineName).get();
                    vaccine.decreaseAvailableDoses(1);
                } catch (SQLException e) {
                    System.out.println("Please try again!");
                    e.printStackTrace();
                }
            }

            PreparedStatement addAppointment =
                    con.prepareStatement("INSERT INTO Appointments VALUES(?, ?, ?, ?, ?)");
            addAppointment.setInt(1, numAppointments);
            addAppointment.setDate(2, d);
            addAppointment.setString(3, currentPatient.getUsername());
            addAppointment.setString(4, availableCaregiver);
            addAppointment.setString(5, vaccineName);
            addAppointment.executeUpdate();
            numAppointments++; // for unique appointment ID

            PreparedStatement getAppointment = con.prepareStatement("SELECT Appointment_ID, " +
                    "Caregiver_Username FROM Appointments WHERE Patient_Username = ? ORDER BY Caregiver_Username");
            getAppointment.setString(1, currentPatient.getUsername());
            ResultSet rs3 = getAppointment.executeQuery();
            while (rs3.next()) {
                System.out.println("Appointment ID: " + rs3.getInt(1)
                        + ", Caregiver username: " + rs3.getString(2));
            }
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // TODO: Extra credit
        // Did Option 1 for extra credit
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) throws SQLException {
        // check 1: the user must be logged in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
        }

        // check 2: the length for tokens need to be exactly 1 to include all information (operation name)
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        if (currentCaregiver != null) {
            try {
                String caregiver = currentCaregiver.getUsername();
                PreparedStatement getCaregiverAppointments = con.prepareStatement("SELECT Appointment_ID, " +
                        "Vaccine_name, Appointment_time, Patient_Username FROM Appointments " +
                        "WHERE Caregiver_Username = ? ORDER BY Appointment_ID");
                getCaregiverAppointments.setString(1, caregiver);
                ResultSet rs = getCaregiverAppointments.executeQuery();
                while (rs.next()) {
                    System.out.println("" + rs.getInt(1) + " " + rs.getString(2) +
                            " " + rs.getString(3) + " " + rs.getString(4));
                }
            } catch (SQLException e) {
                System.out.println("Please try again!");
                e.printStackTrace();
            } finally {
                cm.closeConnection();
            }
        } else {
            try {
                String patient = currentPatient.getUsername();
                PreparedStatement getPatientAppointments = con.prepareStatement("SELECT Appointment_ID, " +
                        "Vaccine_name, Appointment_time, Caregiver_Username FROM Appointments " +
                        "WHERE Patient_Username = ? ORDER BY Appointment_ID");
                getPatientAppointments.setString(1, patient);
                ResultSet rs = getPatientAppointments.executeQuery();
                while (rs.next()) {
                    System.out.println("" + rs.getInt(1) + " " + rs.getString(2) +
                            " " + rs.getString(3) + " " + rs.getString(4));
                }
            } catch (SQLException e) {
                System.out.println("Please try again!");
                e.printStackTrace();
            } finally {
                cm.closeConnection();
            }
        }
    }

    private static void logout(String[] tokens) {
        // check 1: check if the user is not logged in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first.");
            return;
        }

        // check 2: the length for tokens need to be exactly 1 to include all information (operation name)
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }

        currentCaregiver = null;
        currentPatient = null;
        System.out.println("Successfully logged out!");
    }
}