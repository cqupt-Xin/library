package service;

import dao.UserDao;
import model.User;

import java.util.List;

public class UserService {

    private final UserDao userDao = new UserDao();

    public User login(String idStr, String password) {
        try {
            int id = Integer.parseInt(idStr.trim());
            
            User admin = userDao.loginAdmin(id, password);
            if (admin != null) return admin;
            
            return userDao.loginReader(id, password);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public List<User> getAllReaders() {
        return userDao.getAllReaders();
    }

    public boolean disableReader(int readerId) {
        return userDao.updateCardState(readerId, 0);
    }

    public boolean enableReader(int readerId) {
        return userDao.updateCardState(readerId, 1);
    }

    public int register(String name, String passwd, String sex, String birth, String address, String telcode) {
        return userDao.register(name, passwd, sex, birth, address, telcode);
    }
}
