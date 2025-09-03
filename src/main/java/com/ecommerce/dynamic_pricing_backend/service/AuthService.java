package com.ecommerce.dynamic_pricing_backend.service;

import com.ecommerce.dynamic_pricing_backend.dto.AuthResponse;
import com.ecommerce.dynamic_pricing_backend.dto.LoginRequest;
import com.ecommerce.dynamic_pricing_backend.dto.RegisterRequest;
import com.ecommerce.dynamic_pricing_backend.entity.User;
import com.ecommerce.dynamic_pricing_backend.repository.UserRepository;
import com.ecommerce.dynamic_pricing_backend.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final CartService cartService;

    public AuthResponse login(LoginRequest loginRequest){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateToken((User) authentication.getPrincipal());

        User user = (User) authentication.getPrincipal();
        return new AuthResponse(
                jwt,
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name()
        );
    }

    public AuthResponse register(RegisterRequest registerRequest){
        if (userRepository.existsByEmail(registerRequest.getEmail())){
            throw new RuntimeException("Email is already in use!");
        }

        User user = new User();
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setPhone(registerRequest.getPhone());
        user.setAddress(registerRequest.getAddress());
        user.setRole(User.Role.CUSTOMER);

        User savedUser = userRepository.save(user);

        // creating cart for new user
        cartService.createCartForUser(savedUser);

        try {
            String jwt = jwtUtils.generateToken(savedUser);
            System.out.println("JWT: " + jwt);

            return new AuthResponse(
                    jwt,
                    savedUser.getId(),
                    savedUser.getEmail(),
                    savedUser.getFirstName(),
                    savedUser.getLastName(),
                    savedUser.getRole().name()
            );
        } catch (Exception e) {
            e.printStackTrace(); // see the exact error in console
            throw e; // or handle it properly
        }
    }
}
