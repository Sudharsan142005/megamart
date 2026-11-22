package com.megamart.service;

import com.megamart.dto.LoginRequest;
import com.megamart.dto.RegisterRequest;
import com.megamart.dto.UserResponse;
import com.megamart.entity.Cart;
import com.megamart.entity.ERole;
import com.megamart.entity.Role;
import com.megamart.entity.User;
import com.megamart.exception.BadRequestException;
import com.megamart.exception.ResourceNotFoundException;
import com.megamart.exception.UnauthorizedException;
import com.megamart.repository.CartRepository;
import com.megamart.repository.RoleRepository;
import com.megamart.repository.UserRepository;
import com.megamart.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       CartRepository cartRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.cartRepository = cartRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already taken!");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already in use!");
        }

        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                request.getPhone(),
                request.getAddress()
        );

        Set<Role> roles = new HashSet<>();
        String requestedRole = request.getRole() != null ? request.getRole().toUpperCase() : "CUSTOMER";

        if ("SELLER".equals(requestedRole)) {
            Role sellerRole = roleRepository.findByName(ERole.ROLE_SELLER)
                    .orElseThrow(() -> new ResourceNotFoundException("Role ROLE_SELLER not found"));
            roles.add(sellerRole);
            user.setStoreName(request.getStoreName() != null ? request.getStoreName() : request.getFullName() + "'s Store");
            user.setApproved(false);
            user.setApprovalStatus("PENDING");
        } else {
            Role customerRole = roleRepository.findByName(ERole.ROLE_CUSTOMER)
                    .orElseThrow(() -> new ResourceNotFoundException("Role ROLE_CUSTOMER not found"));
            roles.add(customerRole);
            user.setApproved(true);
            user.setApprovalStatus("APPROVED");
        }

        user.setRoles(roles);
        User savedUser = userRepository.save(user);

        // If customer, initialize empty shopping cart
        if (roles.stream().anyMatch(r -> r.getName() == ERole.ROLE_CUSTOMER)) {
            Cart cart = new Cart(savedUser);
            cartRepository.save(cart);
        }

        return mapToUserResponse(savedUser);
    }

    public UserResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(authentication);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isEnabled()) {
            throw new UnauthorizedException("Your account has been disabled by an administrator");
        }

        return mapToUserResponse(user);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @Transactional(readOnly = true)
    public User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("User is not authenticated");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        User user = getCurrentAuthenticatedUser();
        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(RegisterRequest updateRequest) {
        User user = getCurrentAuthenticatedUser();

        if (updateRequest.getFullName() != null && !updateRequest.getFullName().isBlank()) {
            user.setFullName(updateRequest.getFullName());
        }
        if (updateRequest.getPhone() != null) {
            user.setPhone(updateRequest.getPhone());
        }
        if (updateRequest.getAddress() != null) {
            user.setAddress(updateRequest.getAddress());
        }
        if (updateRequest.getStoreName() != null && !updateRequest.getStoreName().isBlank()) {
            user.setStoreName(updateRequest.getStoreName());
        }

        User updatedUser = userRepository.save(user);
        return mapToUserResponse(updatedUser);
    }

    public UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setPhone(user.getPhone());
        response.setAddress(user.getAddress());
        response.setStoreName(user.getStoreName());
        response.setEnabled(user.isEnabled());
        response.setApproved(user.isApproved());
        response.setApprovalStatus(user.getApprovalStatus());
        response.setRoles(user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()));
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
