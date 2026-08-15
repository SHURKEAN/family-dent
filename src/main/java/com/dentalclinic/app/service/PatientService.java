package com.dentalclinic.app.service;

import com.dentalclinic.app.domain.Patient;
import com.dentalclinic.app.repository.PatientRepository;
import com.dentalclinic.app.web.form.PatientForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PatientService {
    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public List<Patient> search(String search, boolean includeArchived) {
        return patientRepository.search(search == null ? "" : search.trim(), includeArchived);
    }

    public List<Patient> activePatients() {
        return patientRepository.findByArchivedFalseOrderByLastNameAscFirstNameAsc();
    }

    public Patient get(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ClinicNotFoundException("Hasta bulunamadı"));
    }

    public PatientForm toForm(Patient patient) {
        PatientForm form = new PatientForm();
        form.setFirstName(patient.getFirstName());
        form.setLastName(patient.getLastName());
        form.setDateOfBirth(patient.getDateOfBirth());
        form.setPhone(patient.getPhone());
        form.setEmail(patient.getEmail());
        form.setAddress(patient.getAddress());
        form.setNotes(patient.getNotes());
        return form;
    }

    @Transactional
    public Patient create(PatientForm form) {
        Patient patient = new Patient();
        copy(form, patient);
        return patientRepository.save(patient);
    }

    @Transactional
    public Patient update(Long id, PatientForm form) {
        Patient patient = get(id);
        copy(form, patient);
        return patient;
    }

    @Transactional
    public void setArchived(Long id, boolean archived) {
        get(id).setArchived(archived);
    }

    public long countActive() {
        return patientRepository.countByArchivedFalse();
    }

    @Transactional
    public Patient resolveForPublicBooking(String firstName, String lastName, String phone) {
        Patient patient = patientRepository.findFirstByPhoneOrderByIdAsc(phone).orElseGet(Patient::new);
        if (patient.getId() == null) {
            patient.setFirstName(firstName.trim());
            patient.setLastName(lastName.trim());
            patient.setPhone(phone);
            patient.setArchived(false);
            return patientRepository.save(patient);
        }
        if (patient.isArchived()) {
            patient.setArchived(false);
        }
        return patient;
    }

    private void copy(PatientForm form, Patient patient) {
        patient.setFirstName(form.getFirstName().trim());
        patient.setLastName(form.getLastName().trim());
        patient.setDateOfBirth(form.getDateOfBirth());
        patient.setPhone(form.getPhone().trim());
        patient.setEmail(blankToNull(form.getEmail()));
        patient.setAddress(blankToNull(form.getAddress()));
        patient.setNotes(blankToNull(form.getNotes()));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
