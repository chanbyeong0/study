package mago.study.domain.message.api;

import lombok.RequiredArgsConstructor;
import mago.study.domain.message.application.MessageService;
import mago.study.domain.message.dto.req.MessageReqDto;
import mago.study.domain.message.dto.res.MessageGetDto;
import mago.study.domain.message.dto.res.MessageSlice;
import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms")
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/{roomId}/messages")
    public ResponseEntity<MessageGetDto> sendMessage(
            @RequestBody MessageReqDto messageReqDto,
            @PathVariable ObjectId roomId
    ) {
        MessageGetDto messageGetDto = messageService.sendMessage(roomId, messageReqDto);
        return ResponseEntity.ok(messageGetDto);
    }

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<MessageSlice> getMessages(
            @PathVariable ObjectId roomId,
            @RequestParam(required = false) String before,
            @RequestParam(defaultValue = "20") int limit
    ) {
        MessageSlice messageHistory = messageService.getMessageHistory(roomId, before, limit);
        return ResponseEntity.ok(messageHistory);
    }
}
