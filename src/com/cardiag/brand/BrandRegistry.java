package com.cardiag.brand;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton registry for all supported vehicle brands.
 * Brands register themselves here so the application can look them up by name.
 */
public final class BrandRegistry {

    private static final BrandRegistry INSTANCE = new BrandRegistry();

    private final Map<String, Brand> brands = new HashMap<>();

    private BrandRegistry() {
        // singleton
    }

    /**
     * Returns the singleton instance of the brand registry.
     *
     * @return the registry instance
     */
    public static BrandRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a brand. If a brand with the same name already exists, it is replaced.
     *
     * @param brand the brand to register
     * @throws IllegalArgumentException if {@code brand} is null
     */
    public void registerBrand(Brand brand) {
        if (brand == null) {
            throw new IllegalArgumentException("Brand must not be null");
        }
        brands.put(brand.getName().toUpperCase(), brand);
    }

    /**
     * Retrieves a brand by name (case-insensitive).
     *
     * @param name the brand name
     * @return the matching {@link Brand}, or {@code null} if not found
     */
    public Brand getBrand(String name) {
        if (name == null) {
            return null;
        }
        return brands.get(name.toUpperCase());
    }

    /**
     * Returns all registered brands.
     *
     * @return an unmodifiable collection of brands
     */
    public Collection<Brand> listBrands() {
        return Collections.unmodifiableCollection(brands.values());
    }
}
