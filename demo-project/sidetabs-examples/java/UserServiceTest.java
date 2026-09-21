package shop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceTest {
    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(new StubUsers());
    }

    @Test
    void findsAUserById() {
        assertTrue(service.find("42").isPresent());
    }

    @Test
    void listsActiveUsers() {
        assertEquals(1, service.listActive().size());
    }

    private static final class StubUsers implements UserRepository {
        private final List<User> users = List.of(new User("42", "Ada", true));

        @Override
        public Optional<User> findById(String id) {
            return users.stream().filter(user -> user.id().equals(id)).findFirst();
        }

        @Override
        public List<User> findAll() {
            return users;
        }
    }
}
