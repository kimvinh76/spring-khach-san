package com.example.demo.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public User registerUser(String username, String password, String fullName, String email, String phone) {
        // Kiểm tra username đã tồn tại
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại!");
        }
        
        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email đã được sử dụng!");
        }
        
        // Kiểm tra phone đã tồn tại
        if (userRepository.existsByPhone(phone)) {
            throw new RuntimeException("Số điện thoại đã được sử dụng!");
        }
        
        // Tạo user mới (trong thực tế nên mã hóa password)
        User user = new User(username, password, fullName, email, phone);
        return userRepository.save(user);
    }
    
    public Optional<User> loginUser(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent() && user.get().getPassword().equals(password) && user.get().getActive()) {
            return user;
        }
        return Optional.empty();
    }
    
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public Optional<User> findByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }
    
    public User saveUser(User user) {
        return userRepository.save(user);
    }
    
    public boolean isUsernameExists(String username) {
        return userRepository.existsByUsername(username);
    }
    
    public boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }
    
    public boolean isPhoneExists(String phone) {
        return userRepository.existsByPhone(phone);
    }
}
