package com.a509.service_payment.item.dto;

import com.a509.service_payment.item.domain.Item;
import com.a509.service_payment.item.enums.Items;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ItemResponseDto {
    private Long id;
    private Items items;

    public ItemResponseDto(Item item){
        this.id = item.getId();
        this.items = item.getName();
    }

    public ItemResponseDto fromEntity(Item item){
        return ItemResponseDto.builder()
                .id(item.getId())
                .items(item.getName())
                .build();
    }
}