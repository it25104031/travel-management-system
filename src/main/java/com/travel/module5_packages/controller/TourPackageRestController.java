package com.travel.module5_packages.controller;

import com.travel.module5_packages.entity.TourPackage;
import com.travel.module5_packages.repository.TourPackageRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/packages")
public class TourPackageRestController {
    private final TourPackageRepository repository;
    public TourPackageRestController(TourPackageRepository repository){this.repository=repository;}
    @GetMapping public List<TourPackage> all(){return repository.findAll();}
    @GetMapping("/{id}") public TourPackage one(@PathVariable Integer id){return repository.findById(id).orElse(null);}
    @PostMapping public TourPackage create(@RequestBody TourPackage p){return repository.save(p);}
    @DeleteMapping("/{id}") public void delete(@PathVariable Integer id){repository.deleteById(id);}
}