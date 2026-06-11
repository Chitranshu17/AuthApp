package com.cutm.AuthApp.ServiceImpl;

import com.cutm.AuthApp.DTO.UserDTO;
import com.cutm.AuthApp.Entity.User;
import com.cutm.AuthApp.Exception.ResourceAlreadyExistsException;
import com.cutm.AuthApp.Exception.ResourceNotFoundException;
import com.cutm.AuthApp.Repository.UserRepository;
import com.cutm.AuthApp.Services.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.cutm.AuthApp.Helpers.userHelper.parseUUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final ModelMapper mapper;
    private final PasswordEncoder passwordEncoder; // Injected the encoder!

    @Override
    @Transactional
    public UserDTO createUser(UserDTO userDTO) {
        if (userDTO.getEmail() == null || userDTO.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is Required");
        }

        if (repository.existsByEmail(userDTO.getEmail())) {
            throw new ResourceAlreadyExistsException("User with this email already exists!");
        }

        User user = mapper.map(userDTO, User.class);

        // FIXED TODO: We can now encode the password during standard registration!
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User savedUser = repository.save(user);
        return mapper.map(savedUser, UserDTO.class);
    }

    @Override
    public UserDTO getUserByEmail(String email) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return mapper.map(user, UserDTO.class);
    }

    @Override
    @Transactional
    public UserDTO updateUser(UserDTO userDTO, String userId) {
        UUID parsedUUID = parseUUID(userId);
        User existingUser = repository.findById(parsedUUID)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with given id"));

        if (userDTO.getName() != null) {
            existingUser.setName(userDTO.getName());
        }
        if (userDTO.getImage() != null) {
            existingUser.setImage(userDTO.getImage());
        }
        if (userDTO.getProvider() != null) {
            existingUser.setProvider(userDTO.getProvider());
        }

        // Encode the new password if they are updating it
        if (userDTO.getPassword() != null) {
            existingUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        User updatedUser = repository.save(existingUser);
        return mapper.map(updatedUser, UserDTO.class);
    }

    @Override
    @Transactional
    public void deleteUser(String userId) {
        UUID id = parseUUID(userId);

        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete. User not found with ID: " + userId);
        }

        repository.deleteById(id);
    }

    @Override
    public UserDTO getUserById(String userId) {
        UUID id = parseUUID(userId);

        User user = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        return mapper.map(user, UserDTO.class);
    }

    @Override
    public List<UserDTO> getAllUsers() {
        return repository.findAll()
                .stream()
                .map(user -> mapper.map(user, UserDTO.class))
                .toList();
    }

    @Override
    public User findOrCreateUser(String email) {
        // 1. Check if the user already exists in the database
        Optional<User> existingUser = repository.findByEmail(email); // Fixed variable name

        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        // 2. If they do not exist, register them automatically
        User newUser = new User();
        newUser.setEmail(email);

        // Extract the part of the email before the '@' symbol to use as a default name
        String defaultName = email.substring(0, email.indexOf("@"));
        newUser.setName(defaultName);

        // 3. Handle the Password Requirement
        String randomPassword = UUID.randomUUID().toString();
        newUser.setPassword(passwordEncoder.encode(randomPassword));

        // 4. Save and return the brand new user
        return repository.save(newUser); // Fixed variable name
    }
}