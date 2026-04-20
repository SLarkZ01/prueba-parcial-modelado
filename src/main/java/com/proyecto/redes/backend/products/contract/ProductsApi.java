package com.proyecto.redes.backend.products.contract;

import com.proyecto.redes.backend.products.dto.CreateProductRequest;
import com.proyecto.redes.backend.products.dto.ProductResponse;
import com.proyecto.redes.backend.products.dto.UpdateProductRequest;
import com.proyecto.redes.backend.shared.api.ApiErrorResponse;
import com.proyecto.redes.backend.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Productos", description = "Operaciones CRUD de productos")
public interface ProductsApi {

    @Operation(summary = "Crear producto", description = "Crea un nuevo producto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Producto creado"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos invalidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request);

    @Operation(summary = "Obtener producto por id", description = "Devuelve un producto por su identificador")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Producto encontrado"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Producto no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    ProductResponse getById(@PathVariable Long id);

    @Operation(summary = "Listar productos", description = "Devuelve una lista paginada de productos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista paginada de productos")
    })
    PageResponse<ProductResponse> getAll(@ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable);

    @Operation(summary = "Actualizar producto", description = "Actualiza un producto existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Producto actualizado"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos invalidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Producto no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    ProductResponse update(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request);

    @Operation(summary = "Eliminar producto", description = "Elimina un producto por su id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Producto eliminado"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Producto no encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    ResponseEntity<Void> delete(@PathVariable Long id);
}
