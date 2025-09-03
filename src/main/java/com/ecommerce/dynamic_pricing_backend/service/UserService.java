package com.ecommerce.dynamic_pricing_backend.service;

import com.ecommerce.dynamic_pricing_backend.entity.User;
import com.ecommerce.dynamic_pricing_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;

    public User getCurrentUser(){
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public User findById(Long id){
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: "+id));
    }

    public User findByEmail(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: "+email));
    }

    public List<User> findAllUsers(){
        return userRepository.findAll();
    }

    public User updateUser(Long id, User userDetails){
        User user = findById(id);

        user.setFirstName(userDetails.getFirstName());
        user.setLastName(userDetails.getLastName());
        user.setPhone(userDetails.getPhone());
        user.setAddress(userDetails.getAddress());

        return userRepository.save(user);
    }

    public boolean isFirstTimeBuyer(Long userId) {
        return userRepository.isFirstTimeBuyer(userId);
    }

    public long getOrderCount(Long userId) {
        return userRepository.countOrdersByUserId(userId);
    }
}
