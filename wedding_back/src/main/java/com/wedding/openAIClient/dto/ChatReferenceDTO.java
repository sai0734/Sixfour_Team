package com.wedding.openAIClient.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatReferenceDTO {

    private String type;

    private Long id;

    private String name;

    private String imageUrl;

    private String priceLabel;

    private String link;

}
