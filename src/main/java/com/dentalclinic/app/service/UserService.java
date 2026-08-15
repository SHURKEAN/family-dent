package com.dentalclinic.app.service;

import com.dentalclinic.app.domain.Role;
import com.dentalclinic.app.domain.User;
import com.dentalclinic.app.repository.UserRepository;
import com.dentalclinic.app.web.form.UserForm;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> all() { return userRepository.findAllByOrderByLastNameAscFirstNameAsc(); }
    public List<User> activeDentists() {
        return userRepository.findByRoleAndActiveTrueOrderByLastNameAscFirstNameAsc(Role.DENTIST);
    }
    public User get(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ClinicNotFoundException("Çalışan bulunamadı"));
    }
    public User getForUpdate(Long id) {
        return userRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ClinicNotFoundException("Çalışan bulunamadı"));
    }
    public User getByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ClinicNotFoundException("Çalışan bulunamadı"));
    }
    public boolean usernameExists(String username) {
        return username != null && userRepository.existsByUsernameIgnoreCase(username.trim());
    }

    @Transactional
    public User create(UserForm form) {
        if (usernameExists(form.getUsername())) {
            throw new BusinessRuleException("Bu kullanıcı adı zaten kullanılıyor");
        }
        User user = new User();
        user.setUsername(form.getUsername().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setFirstName(form.getFirstName().trim());
        user.setLastName(form.getLastName().trim());
        user.setRole(form.getRole());
        user.setActive(true);
        return userRepository.save(user);
    }

    @Transactional
    public void setActive(Long id, boolean active, String currentUsername) {
        User user = get(id);
        if (user.getUsername().equalsIgnoreCase(currentUsername) && !active) {
            throw new BusinessRuleException("Kendi hesabınızı pasifleştiremezsiniz");
        }
        user.setActive(active);
    }

    @Transactional
    public void resetPassword(Long id, String password) {
        get(id).setPasswordHash(passwordEncoder.encode(password));
    }
}
