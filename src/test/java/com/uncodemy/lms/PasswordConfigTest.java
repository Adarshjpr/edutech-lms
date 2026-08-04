//  package com.uncodemy.lms;

// @SpringBootTest
// class PasswordConfigTest {

//     @Autowired PasswordEncoder encoder;

//     @Test
//     void bcryptKaamKarRahaHai() {
//         String plain = "Kf7@mQx2";
//         String hash  = encoder.encode(plain);

//         assertNotEquals(plain, hash);           // hash ban gaya
//         assertEquals(60, hash.length());        // BCrypt hamesha 60 char
//         assertTrue(encoder.matches(plain, hash));       // sahi password
//         assertFalse(encoder.matches("galat", hash));    // galat password
//     }
// }