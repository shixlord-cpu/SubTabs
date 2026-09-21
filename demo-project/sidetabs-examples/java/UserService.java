package shop;

import java.util.List;
import java.util.Optional;

public class UserService {
    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    public Optional<User> find(String id) {
        return users.findById(id);
    }

    public List<User> listActive() {
        return users.findAll().stream().filter(User::active).toList();
    }
}
