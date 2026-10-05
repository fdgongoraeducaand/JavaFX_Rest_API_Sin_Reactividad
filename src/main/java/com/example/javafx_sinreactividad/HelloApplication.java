package com.example.javafx_sinreactividad;

import com.example.javafx_sinreactividad.modelos.Usuario;
import com.example.javafx_sinreactividad.modelos.UsuarioRepository;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class HelloApplication extends Application {
    private final UsuarioRepository repository = new UsuarioRepository();

    // Componentes de visualización
    private TableView<Usuario> tabla;
    private Label lblEstado;

    // Campos del formulario
    private TextField txtId;
    private TextField txtNombre;
    private TextField txtUsername;
    private TextField txtEmail;
    private TextField txtTelefono;

    // Botones de acción
    private Button btnGuardar;
    private Button btnEliminar;
    private Button btnLimpiar;
    private Button btnDescargarApi;

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));

        // 1. Configuración de tabla
        tabla = new TableView<>();
        configurarColumnas();

        // Al seleccionar una fila, cargamos imperativamente los datos en el formulario
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, seleccionado) -> {
            if (seleccionado != null) {
                cargarEnFormulario(seleccionado);
            }
        });

        // 2. Construcción del Formulario y Botonera
        VBox panelLateral = crearPanelFormulario();
        HBox barraSuperior = crearBarraSuperior();

        root.setTop(barraSuperior);
        root.setCenter(tabla);
        root.setRight(panelLateral);
        BorderPane.setMargin(panelLateral, new Insets(0, 0, 0, 12));

        Scene scene = new Scene(root, 920, 520);
        primaryStage.setTitle("CRUD en Memoria + REST API (Enfoque Imperativo / Sin Reactividad)");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Mapeo de columnas con ReadOnlyObjectWrapper y lambdas directas al POJO.
     */
    private void configurarColumnas() {
        TableColumn<Usuario, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId()));
        colId.setPrefWidth(50);

        TableColumn<Usuario, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getName()));
        colNombre.setPrefWidth(160);

        TableColumn<Usuario, String> colUsername = new TableColumn<>("Usuario");
        colUsername.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getUsername()));
        colUsername.setPrefWidth(110);

        TableColumn<Usuario, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getEmail()));
        colEmail.setPrefWidth(170);

        TableColumn<Usuario, String> colTelefono = new TableColumn<>("Teléfono");
        colTelefono.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getPhone()));
        colTelefono.setPrefWidth(130);

        tabla.getColumns().addAll(colId, colNombre, colUsername, colEmail, colTelefono);
    }

    private HBox crearBarraSuperior() {
        btnDescargarApi = new Button("Recargar Datos de REST API");
        btnDescargarApi.setOnAction(e -> ejecutarCargaRest());

        lblEstado = new Label("Almacén en memoria listo. Puedes crear registros o consultar la API.");

        HBox barra = new HBox(15, btnDescargarApi, lblEstado);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setPadding(new Insets(0, 0, 10, 0));
        return barra;
    }

    private VBox crearPanelFormulario() {
        VBox panel = new VBox(8);
        panel.setPrefWidth(260);

        Label lblTitulo = new Label("Gestión de Usuario");
        lblTitulo.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        txtId = new TextField();
        txtId.setPromptText("Autogenerado");
        txtId.setDisable(true); // El ID es de solo lectura

        txtNombre = new TextField();
        txtNombre.setPromptText("Nombre completo");

        txtUsername = new TextField();
        txtUsername.setPromptText("Nombre de usuario");

        txtEmail = new TextField();
        txtEmail.setPromptText("correo@dominio.com");

        txtTelefono = new TextField();
        txtTelefono.setPromptText("Teléfono");

        btnGuardar = new Button("Guardar");
        btnGuardar.setDefaultButton(true);
        btnGuardar.setOnAction(e -> handleGuardar());

        btnEliminar = new Button("Eliminar");
        btnEliminar.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white;");
        btnEliminar.setOnAction(e -> handleEliminar());

        btnLimpiar = new Button("Limpiar");
        btnLimpiar.setOnAction(e -> handleLimpiar());

        HBox acciones = new HBox(6, btnGuardar, btnEliminar, btnLimpiar);

        panel.getChildren().addAll(
                lblTitulo,
                new Label("ID:"), txtId,
                new Label("Nombre:"), txtNombre,
                new Label("Username:"), txtUsername,
                new Label("Email:"), txtEmail,
                new Label("Teléfono:"), txtTelefono,
                new Separator(),
                acciones
        );
        return panel;
    }

    // =========================================================================
    // OPERACIONES CRUD IMPERATIVAS (SIN BINDINGS NI PROPIEDADES OBSERVABLES)
    // =========================================================================

    /**
     * CREATE / UPDATE: Si txtId está vacío, crea un nuevo registro.
     * Si contiene un ID, modifica la instancia existente en el repositorio en memoria.
     */
    private void handleGuardar() {
        String nombre = txtNombre.getText().trim();
        String username = txtUsername.getText().trim();
        String email = txtEmail.getText().trim();
        String telefono = txtTelefono.getText().trim();

        if (nombre.isEmpty() || email.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Validación", "Nombre y Email son requeridos.");
            return;
        }

        if (txtId.getText().isEmpty()) {
            // CREATE
            Usuario nuevo = new Usuario(0, nombre, username, email, telefono);
            repository.crear(nuevo);
            lblEstado.setText("Usuario creado en memoria local.");
        } else {
            // UPDATE
            int id = Integer.parseInt(txtId.getText());
            Usuario editado = new Usuario(id, nombre, username, email, telefono);
            repository.actualizar(editado);
            lblEstado.setText("Usuario ID " + id + " actualizado en memoria.");
        }

        sincronizarTabla();
        handleLimpiar();
    }

    /**
     * DELETE: Elimina el elemento seleccionado en la tabla y en la memoria.
     */
    private void handleEliminar() {
        if (txtId.getText().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención", "Seleccione un usuario para eliminar.");
            return;
        }

        int id = Integer.parseInt(txtId.getText());
        boolean eliminado = repository.eliminar(id);

        if (eliminado) {
            lblEstado.setText("Usuario ID " + id + " eliminado.");
            sincronizarTabla();
            handleLimpiar();
        }
    }

    /**
     * READ: Reasigna de forma manual y explícita la colección estándar al TableView.
     */
    private void sincronizarTabla() {
        // Al no existir ObservableList que notifique cambios automáticamente,
        // volcamos la lista java.util.List mediante setAll() imperativo.
        tabla.getItems().setAll(repository.listarTodos());
    }

    private void cargarEnFormulario(Usuario u) {
        txtId.setText(String.valueOf(u.getId()));
        txtNombre.setText(u.getName());
        txtUsername.setText(u.getUsername());
        txtEmail.setText(u.getEmail());
        txtTelefono.setText(u.getPhone());
    }

    private void handleLimpiar() {
        txtId.clear();
        txtNombre.clear();
        txtUsername.clear();
        txtEmail.clear();
        txtTelefono.clear();
        tabla.getSelectionModel().clearSelection();
    }

    private void ejecutarCargaRest() {
        btnDescargarApi.setDisable(true);
        lblEstado.setText("Consultando REST API...");

        // Desacople de red fuera del JavaFX Application Thread para no congelar la UI
        new Thread(() -> {
            try {
                repository.cargarDesdeApi();
                Platform.runLater(() -> {
                    sincronizarTabla();
                    lblEstado.setText("Datos sincronizados desde REST API (" + repository.listarTodos().size() + " registros).");
                    btnDescargarApi.setDisable(false);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    lblEstado.setText("Fallo en la comunicación REST.");
                    btnDescargarApi.setDisable(false);
                    mostrarAlerta(Alert.AlertType.ERROR, "Error REST", ex.getMessage());
                });
            }
        }).start();
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
