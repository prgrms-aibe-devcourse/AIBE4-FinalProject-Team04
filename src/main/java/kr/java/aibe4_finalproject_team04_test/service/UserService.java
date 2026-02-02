package kr.java.aibe4_finalproject_team04_test.service;


import kr.java.aibe4_finalproject_team04_test.dto.UserDto;
import kr.java.aibe4_finalproject_team04_test.entity.Order;
import kr.java.aibe4_finalproject_team04_test.entity.User;
import kr.java.aibe4_finalproject_team04_test.repository.OrderRepository;
import kr.java.aibe4_finalproject_team04_test.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    private final String ADMIN_SECRET_KEY = "test12223";

    public void registerUser(UserDto userDto) {
        System.out.println("회원가입 요청 받음! 비번: " + userDto.getPassword());

        User user = new User();
        user.setEmail(userDto.getEmail());

        user.setPassword(userDto.getPassword());

        if (userDto.getName().equals("ADMIN")) {
            if (userDto.getSecretKey().equals(ADMIN_SECRET_KEY)) {
                user.setRole("ADMIN");
            }
        } else {
            user.setRole("USER");
        }

        userRepository.save(user);
    }

    public void chargePoint(Long userId, int amount) {
        User user = userRepository.findById(userId).orElse(null);

        user.setPoint(user.getPoint() + amount);

        userRepository.save(user);
    }

    public List<String> getAllUserOrderStats() {
        List<User> users = userRepository.findAll();
        List<String> stats = new ArrayList<>();

        for (User user : users) {
            List<Order> orders = orderRepository.findByUserId(user.getId());

            int total = 0;
            for (Order order : orders) {
                total += order.getAmount();
            }
            stats.add(user.getName() + "님의 총 주문액: " + total);
        }

        return stats;
    }
}