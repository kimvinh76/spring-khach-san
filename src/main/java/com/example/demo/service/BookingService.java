package com.example.demo.service;

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
}
