package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.model.RoomType;
import com.example.demo.repository.RoomTypeRepository;

@Service
public class RoomTypeService {
    private final RoomTypeRepository roomTypeRepository;

    public RoomTypeService(RoomTypeRepository roomTypeRepository) {
        this.roomTypeRepository = roomTypeRepository;
    }

    public List<RoomType> getAllRoomTypes() {
        return roomTypeRepository.findAll();
    }

    public Optional<RoomType> getRoomTypeById(Long id) {
        return roomTypeRepository.findById(id);
    }

    public RoomType saveRoomType(RoomType roomType) {
        return roomTypeRepository.save(roomType);
    }

    public void deleteRoomType(Long id) {
        roomTypeRepository.deleteById(id);
    }
}
