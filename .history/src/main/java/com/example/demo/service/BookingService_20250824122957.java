package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.model.Booking;
import com.example.demo.repository.BookingRepository;

@Service
public class BookingService {
    public List<Booking> findByUserId(Long userId) {
        return bookingRepository.findByUserId(userId);
    }
    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public List<Booking> findByUserIdAndStatusList(Long userId, List<String> statuses) {
        return bookingRepository.findByUserIdAndStatusIn(userId, statuses);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public List<Booking> getBookingsByUserId(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }

    public Booking saveBooking(Booking booking) {
        return bookingRepository.save(booking);
    }

    public void deleteBooking(Long id) {
        bookingRepository.deleteById(id);
    }

    public List<Booking> getBookingsByStatusAndUser(String status, Long userId) {
        return bookingRepository.findByUserIdAndStatusContaining(userId, status);
    }
    
    public List<Booking> findByUserIdAndStatusContaining(Long userId, String status) {
        return bookingRepository.findByUserIdAndStatusContaining(userId, status);
    }

    // Thêm các method cần thiết cho AdminBookingController
    public List<Booking> findAll() {
        return bookingRepository.findAll();
    }

    public Optional<Booking> findById(Long id) {
        return bookingRepository.findById(id);
    }

    public Booking save(Booking booking) {
        return bookingRepository.save(booking);
    }

    public void deleteById(Long id) {
        bookingRepository.deleteById(id);
    }
    
    public List<Booking> findByPhone(String phone) {
        return bookingRepository.findByPhone(phone);
    }
    
    public List<Booking> findByCustomerName(String customerName) {
        return bookingRepository.findByCustomerName(customerName);
    }
    
    public List<Booking> findByCustomerNameContainingIgnoreCase(String customerName) {
        return bookingRepository.findByCustomerNameContainingIgnoreCase(customerName);
    }
    
    public List<Booking> findByStatusContaining(String status) {
        return bookingRepository.findByStatusContaining(status);
    }

    /**
     * Test helper used only by PaymentIntegrationTest to quickly create a minimal Booking.
     * Avoids having to duplicate booking creation logic inside the test.
     */
    public Booking createSampleBookingForTest() {
        Booking b = new Booking();
        b.setCustomerName("Test User");
        b.setEmail("test@example.com");
        b.setPhone("0000000000");
        b.setGuestCount(1);
        b.setNote("Test booking");
        LocalDate today = LocalDate.now();
        b.setCheckIn(today.plusDays(1));
        b.setCheckOut(today.plusDays(2));
        b.setBookingDate(LocalDateTime.now());
        b.setTotalAmount(BigDecimal.valueOf(100000));
        b.setStatus("PENDING");
        // room & userId left null intentionally – not required for payment initiation test
        return bookingRepository.save(b);
    }
}
