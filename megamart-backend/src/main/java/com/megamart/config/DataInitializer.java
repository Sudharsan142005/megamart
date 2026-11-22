package com.megamart.config;

import com.megamart.entity.*;
import com.megamart.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           CategoryRepository categoryRepository,
                           ProductRepository productRepository,
                           CartRepository cartRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Seed Roles
        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(ERole.ROLE_ADMIN)));
        Role sellerRole = roleRepository.findByName(ERole.ROLE_SELLER)
                .orElseGet(() -> roleRepository.save(new Role(ERole.ROLE_SELLER)));
        Role customerRole = roleRepository.findByName(ERole.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(new Role(ERole.ROLE_CUSTOMER)));

        logger.info("Roles initialized: ADMIN, SELLER, CUSTOMER");

        // 2. Seed Admin User
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User("admin", "admin@megamart.com", passwordEncoder.encode("Admin@123"), "MegaMart Administrator", "+1-800-000-0001", "MegaMart Headquarters, 1 Silicon Way");
            admin.setRoles(new HashSet<>(Set.of(adminRole)));
            admin.setEnabled(true);
            admin.setApproved(true);
            admin.setApprovalStatus("APPROVED");
            userRepository.save(admin);
            logger.info("Default Admin created: admin@megamart.com / Admin@123");
        }

        // 3. Seed Sample Approved Seller
        User seller = null;
        if (!userRepository.existsByUsername("seller1")) {
            seller = new User("seller1", "seller@megamart.com", passwordEncoder.encode("Seller@123"), "Apex Electronics Store", "+1-800-555-0199", "742 Evergreen Terrace, Tech Park");
            seller.setStoreName("Apex Tech Solutions");
            seller.setRoles(new HashSet<>(Set.of(sellerRole)));
            seller.setEnabled(true);
            seller.setApproved(true);
            seller.setApprovalStatus("APPROVED");
            seller = userRepository.save(seller);
            logger.info("Sample Approved Seller created: seller@megamart.com / Seller@123");
        } else {
            seller = userRepository.findByUsername("seller1").orElse(null);
        }

        // 4. Seed Sample Pending Seller (for testing admin approval workflow)
        if (!userRepository.existsByUsername("pending_seller")) {
            User pendingSeller = new User("pending_seller", "newbrand@megamart.com", passwordEncoder.encode("Seller@123"), "Nova Fashion Brands", "+1-800-555-0288", "120 Fashion Blvd, Suite 400");
            pendingSeller.setStoreName("Nova Apparel Co.");
            pendingSeller.setRoles(new HashSet<>(Set.of(sellerRole)));
            pendingSeller.setEnabled(true);
            pendingSeller.setApproved(false);
            pendingSeller.setApprovalStatus("PENDING");
            userRepository.save(pendingSeller);
            logger.info("Sample Pending Seller created: newbrand@megamart.com / Seller@123");
        }

        // 5. Seed Sample Customer
        if (!userRepository.existsByUsername("customer1")) {
            User customer = new User("customer1", "customer@megamart.com", passwordEncoder.encode("Customer@123"), "John Customer", "+1-555-014-9922", "100 Main Street, Apt 4B, New York, NY 10001");
            customer.setRoles(new HashSet<>(Set.of(customerRole)));
            customer.setEnabled(true);
            customer.setApproved(true);
            customer.setApprovalStatus("APPROVED");
            User savedCustomer = userRepository.save(customer);

            Cart cart = new Cart(savedCustomer);
            cartRepository.save(cart);
            logger.info("Sample Customer created: customer@megamart.com / Customer@123");
        }

        // 6. Seed Categories
        Category electronics = seedCategory("Electronics", "Latest gadgets, computers, audio and smart home tech", "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&auto=format&fit=crop&q=80");
        Category fashion = seedCategory("Fashion & Apparel", "Trendy clothing, footwear and everyday accessories", "https://images.unsplash.com/photo-1445205170230-053b83016050?w=600&auto=format&fit=crop&q=80");
        Category home = seedCategory("Home & Kitchen", "Cookware, modern furniture and smart appliances", "https://images.unsplash.com/photo-1556911220-e15b29be8c8f?w=600&auto=format&fit=crop&q=80");
        Category books = seedCategory("Books & Stationery", "Best-selling novels, software design and educational titles", "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=600&auto=format&fit=crop&q=80");
        Category beauty = seedCategory("Beauty & Personal Care", "Organic skincare, wellness and personal grooming essentials", "https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?w=600&auto=format&fit=crop&q=80");

        // 7. Seed Sample Products
        if (productRepository.count() == 0 && seller != null) {
            seedProduct("Ultra-Clear Noise Cancelling Headphones", "Experience immersive, studio-grade sound with active noise cancellation, 40-hour battery life, and ultra-plush memory foam ear cups.", new BigDecimal("149.99"), 25, "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600&auto=format&fit=crop&q=80", electronics, seller);
            seedProduct("4K Ultra-Slim 27-inch IPS Monitor", "Stunning color accuracy, 144Hz refresh rate, USB-C 65W charging and ergonomic tilt-and-swivel stand for productivity and gaming.", new BigDecimal("399.99"), 15, "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=600&auto=format&fit=crop&q=80", electronics, seller);
            seedProduct("Mechanical RGB Keyboard - Linear Red Switches", "Anodized aluminum frame, custom PBT double-shot keycaps, hot-swappable switches and per-key customizable RGB backlighting.", new BigDecimal("89.50"), 40, "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=600&auto=format&fit=crop&q=80", electronics, seller);
            seedProduct("Smart Fitness & Health Tracker Watch", "All-day heart rate monitoring, SPO2 tracking, GPS running analytics, 5ATM water resistance and 10-day battery life.", new BigDecimal("199.00"), 30, "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80", electronics, seller);
            seedProduct("Ergonomic Mesh Office Executive Chair", "Engineered for lumbar support with 4D adjustable armrests, breathable high-density mesh and multi-lock recline mechanism.", new BigDecimal("249.99"), 12, "https://images.unsplash.com/photo-1580481077195-c22ae24f5a89?w=600&auto=format&fit=crop&q=80", home, seller);
            seedProduct("10-Piece Stainless Steel Tri-Ply Cookware Set", "Professional grade cookware featuring induction-ready aluminum core bases and stay-cool ergonomic riveted handles.", new BigDecimal("129.95"), 20, "https://images.unsplash.com/photo-1584990347449-399478f682e0?w=600&auto=format&fit=crop&q=80", home, seller);
            seedProduct("Clean Code: Agile Software Craftsmanship", "A handbook of agile software craftsmanship by Robert C. Martin. Essential reading for every software engineer.", new BigDecimal("44.99"), 50, "https://images.unsplash.com/photo-1532012164546-f432f2e3777f?w=600&auto=format&fit=crop&q=80", books, seller);
            seedProduct("Organic Botanical Revitalizing Face Serum", "Infused with Hyaluronic Acid, Vitamin C and cold-pressed botanical extracts for daily rejuvenation and radiant skin.", new BigDecimal("34.50"), 35, "https://images.unsplash.com/photo-1608248597359-54d9241951f2?w=600&auto=format&fit=crop&q=80", beauty, seller);

            logger.info("Seeded 8 sample products in catalog");
        }
    }

    private Category seedCategory(String name, String description, String imageUrl) {
        return categoryRepository.findByName(name)
                .orElseGet(() -> categoryRepository.save(new Category(name, description, imageUrl)));
    }

    private void seedProduct(String name, String desc, BigDecimal price, int stock, String img, Category cat, User seller) {
        Product p = new Product(name, desc, price, stock, img, cat, seller);
        productRepository.save(p);
    }
}
