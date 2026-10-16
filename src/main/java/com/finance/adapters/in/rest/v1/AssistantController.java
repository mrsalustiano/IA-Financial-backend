package com.finance.adapters.in.rest.v1;

import com.finance.application.dto.AskAssistantDto;
import com.finance.application.dto.AssistantMessageResponseDto;
import com.finance.application.mapper.AssistantMessageDtoMapper;
import com.finance.application.query.PageResult;
import com.finance.domain.model.AssistantMessage;
import com.finance.domain.ports.in.AskAssistantUseCase;
import com.finance.domain.ports.in.GetAssistantMessageByIdUseCase;
import com.finance.domain.ports.in.ListAssistantMessagesUseCase;
import com.finance.shared.constants.AppConstants;
import com.finance.shared.response.ApiResponseDto;
import com.finance.shared.response.PageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(AppConstants.API_V1 + "/assistant/questions")
@Tag(name = "Assistente Financeiro", description = "Assistente financeiro com IA usando os dados cadastrados")
public class AssistantController {

    private final AskAssistantUseCase askAssistantUseCase;
    private final GetAssistantMessageByIdUseCase getAssistantMessageByIdUseCase;
    private final ListAssistantMessagesUseCase listAssistantMessagesUseCase;
    private final AssistantMessageDtoMapper messageDtoMapper;

    public AssistantController(AskAssistantUseCase askAssistantUseCase,
                               GetAssistantMessageByIdUseCase getAssistantMessageByIdUseCase,
                               ListAssistantMessagesUseCase listAssistantMessagesUseCase,
                               AssistantMessageDtoMapper messageDtoMapper) {
        this.askAssistantUseCase = askAssistantUseCase;
        this.getAssistantMessageByIdUseCase = getAssistantMessageByIdUseCase;
        this.listAssistantMessagesUseCase = listAssistantMessagesUseCase;
        this.messageDtoMapper = messageDtoMapper;
    }

    @Operation(summary = "Fazer uma pergunta ao assistente financeiro")
    @PostMapping
    public ResponseEntity<ApiResponseDto<AssistantMessageResponseDto>> ask(
        @Valid @RequestBody AskAssistantDto request) {
        AssistantMessage message = askAssistantUseCase.handle(messageDtoMapper.toCommand(request));
        return ResponseEntity
            .created(URI.create(AppConstants.API_V1 + "/assistant/questions/" + message.getId()))
            .body(ApiResponseDto.of(messageDtoMapper.toResponseDto(message), AppConstants.MESSAGE_SUCCESS));
    }

    @Operation(summary = "Buscar pergunta/resposta por id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<AssistantMessageResponseDto>> findById(@PathVariable String id) {
        AssistantMessage message = getAssistantMessageByIdUseCase.handle(id);
        return ResponseEntity
            .ok(ApiResponseDto.of(messageDtoMapper.toResponseDto(message), AppConstants.MESSAGE_SUCCESS));
    }

    @Operation(summary = "Listar histórico de perguntas com paginação")
    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<AssistantMessageResponseDto>>> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        PageResult<AssistantMessage> result = listAssistantMessagesUseCase.handle(page, size);
        List<AssistantMessageResponseDto> content = result.content().stream().map(messageDtoMapper::toResponseDto)
            .toList();
        return ResponseEntity.ok(ApiResponseDto.of(new PageResponseDto<>(content, result.page(), result.size(),
            result.totalElements(), result.totalPages()), AppConstants.MESSAGE_SUCCESS));
    }
}
