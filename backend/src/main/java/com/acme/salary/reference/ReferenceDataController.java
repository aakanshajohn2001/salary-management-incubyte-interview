package com.acme.salary.reference;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Small read-only lookups the UI needs to populate filter dropdowns
 * (department/country) without hardcoding or duplicating the seeded
 * reference data client-side.
 */
@RestController
@RequestMapping("/api/reference")
public class ReferenceDataController {

    private final DepartmentRepository departmentRepository;
    private final CountryRepository countryRepository;

    public ReferenceDataController(DepartmentRepository departmentRepository, CountryRepository countryRepository) {
        this.departmentRepository = departmentRepository;
        this.countryRepository = countryRepository;
    }

    @GetMapping("/departments")
    public List<DepartmentDto> departments() {
        return departmentRepository.findAll(Sort.by("name")).stream().map(DepartmentDto::from).toList();
    }

    @GetMapping("/countries")
    public List<CountryDto> countries() {
        return countryRepository.findAll(Sort.by("name")).stream().map(CountryDto::from).toList();
    }
}
