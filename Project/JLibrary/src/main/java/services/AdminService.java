package services;

import dao.UserDAO;
import models.User;
import java.sql.SQLException;
import java.util.List;

public class AdminService {
    public static void addLibrarian(String username, String password) throws Exception {
        if (UserDAO.findUserByUsername(username) != null) {
            throw new Exception("Użytkownik już istnieje");
        }
        User librarian = new User(username, password, "LIBRARIAN");
        UserDAO.addUser(librarian);
    }

    public static boolean deleteUser(int userId) throws Exception {
        try {
            User user = UserDAO.findUserById(userId);
            if (user == null) {
                throw new Exception("Użytkownik nie istnieje");
            }

            if ("ADMIN".equals(user.getRole())) {
                return false; // Nie można usunąć administratora
            }

            return UserDAO.deleteUser(userId);
        } catch (SQLException e) {
            throw new Exception("Błąd bazy danych");
        }
    }

    public static List<User> getAllUsers() throws Exception {
        try {
            return UserDAO.getAllUsers();
        } catch (SQLException e) {
            throw new Exception("Błąd bazy danych");
        }
    }
}