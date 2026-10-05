# Comparativa de Arquitectura en JavaFX: Enfoque Imperativo vs. Enfoque Reactivo

Este documento analiza las diferencias técnicas, ventajas e inconvenientes entre el desarrollo de interfaces en JavaFX mediante un **modelo imperativo clásico** (sin propiedades observables ni bindings) frente al **paradigma reactivo oficial de JavaFX** (`ObservableList`, `JavaFX Properties` y `Bindings`).

---

## 1. Cuadro Comparativo General

| Criterio | Enfoque Imperativo (Clásico) | Enfoque Reactivo (Declarativo) |
| --- | --- | --- |
| **Modelo de Dominio** | **POJO estándar**: tipos primitivos (`int`, `String`) y getters/setters tradicionales. | **JavaFX Bean**: propiedades observables (`IntegerProperty`, `StringProperty`). |
| **Sincronización de UI** | **Manual y explícita**: requiere llamar a `tabla.getItems().setAll(...)` o `tabla.refresh()`. | **Automática y desacoplada**: la vista se suscribe a los cambios del modelo (`ListChangeListener`). |
| **Actualización de Celdas** | Repintado forzado de la tabla o recarga completa de la lista. | Repintado atómico de la celda afectada mediante extractores de propiedades en `ObservableList`. |
| **Validaciones y Botones** | Comprobaciones condicionales dispersas en controladores o manejadores de eventos (`if`). | **Bindings booleanos declarativos**: se enlaza `btn.disableProperty().bind(...)` a expresiones lógicas. |
| **Operaciones Asíncronas (REST)** | Hilo manual (`Thread`) y despacho explícito a la UI con `Platform.runLater()`. | `javafx.concurrent.Task` con propiedades observables nativas (`messageProperty`, `runningProperty`). |
| **Consumo de Memoria** | **Mínimo**: almacena únicamente valores directos en el *Heap*. | **Mayor**: cada atributo envuelve el valor en un objeto `Property`. |
| **Integración con Librerías** | Inmediata con serializadores (Jackson), JPA/Hibernate y JDBC sin adaptaciones. | Requiere configuración de acceso por reflexión (`module-info.java`) y anotaciones de mapeo (`@JsonProperty`). |

---

## 2. Ventajas e Inconvenientes

### Enfoque Imperativo (Sin Reactividad)

* **Ventajas:**
* **Baja huella de memoria:** Ideal para procesar volúmenes masivos de registros o en dispositivos con recursos de hardware limitados.
* **Compatibilidad directa:** No genera fricción con librerías externas de persistencia o serialización JSON.
* **Curva de entrada baja:** Sigue el flujo secuencial tradicional paso a paso.


* **Inconvenientes:**
* **Alto acoplamiento:** El controlador debe recordar explícitamente actualizar cada control gráfico tras cualquier mutación del modelo.
* **Propensión a inconsistencias:** Si se olvida un refresco manual, la interfaz muestra datos desactualizados respecto al estado en memoria.
* **Código repetitivo (*Boilerplate*):** Exceso de lógica manual para habilitar/deshabilitar botones y validar entradas.



### Enfoque Reactivo (Con Reactividad)

* **Ventajas:**
* **Consistencia garantizada:** Los componentes de la interfaz reflejan el estado del modelo en tiempo real de forma automática.
* **Validaciones fluidas:** La habilitación de controles y cálculos derivados responden al instante según el usuario interactúa.
* **Eficiencia en repintado:** Las colecciones observables con extractores solo actualizan la celda que ha mutado en lugar de la tabla completa.


* **Inconvenientes:**
* **Sobrecarga de objetos (*Heap overhead*):** Cada propiedad es una instancia en memoria, lo que impacta en el recolector de basura con cientos de miles de entidades.
* **Configuración modular estricta:** En aplicaciones modulares (JPMS), se debe abrir explícitamente el paquete del modelo a `javafx.base` y a los serializadores.



---

## 3. Comparativa de Código

### A. Modelo de Datos: POJO vs. JavaFX Bean

#### Enfoque Imperativo (POJO puro)

```java
public class Usuario {
    private int id;
    private String name;

    public Usuario() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

```

#### Enfoque Reactivo (JavaFX Observable Bean)

```java
public class Usuario {
    private final IntegerProperty id = new SimpleIntegerProperty(this, "id", 0);
    private final StringProperty name = new SimpleStringProperty(this, "name", "");

    public Usuario() {}

    // Getter tradicional
    public String getName() { return name.get(); }
    // Setter tradicional
    public void setName(String name) { this.name.set(name); }
    // Propiedad observable para binding
    public StringProperty nameProperty() { return name; }
}

```

---

### B. Vinculación y Actualización de Tablas (`TableView`)

#### Enfoque Imperativo (Extracción de solo lectura y recarga manual)

```java
// Configuración manual sin PropertyValueFactory
colNombre.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getName()));

// Al modificar un objeto en memoria:
usuario.setName("Carlos Gomez");
// La tabla NO se entera; se debe forzar la sincronización completa:
tabla.getItems().setAll(repositorio.listarTodos());

```

#### Enfoque Reactivo (ObservableList con Extractor de Propiedades)

```java
// Colección observable con extractor de campos internos
ObservableList<Usuario> lista = FXCollections.observableArrayList(
    usuario -> new Observable[]{ usuario.nameProperty() }
);
tabla.setItems(lista);
colNombre.setCellValueFactory(new PropertyValueFactory<>("name"));

// Al modificar la propiedad:
usuario.setName("Carlos Gomez");
// ¡La celda específica de la tabla se repinta sola automáticamente!

```

---

### C. Validación y Habilitación de Controles (Botones)

#### Enfoque Imperativo (Comprobación condicional en el evento)

```java
private void handleGuardar() {
    // La validación solo ocurre al pulsar el botón
    if (txtNombre.getText().trim().isEmpty() || txtEmail.getText().trim().isEmpty()) {
        mostrarAlerta("Campos obligatorios vacíos.");
        return;
    }
    // Lógica de guardado...
}

```

#### Enfoque Reactivo (Declarative Data Binding)

```java
// El botón responde en tiempo real a las pulsaciones de teclado en los inputs
BooleanBinding formularioInvalido = Bindings.createBooleanBinding(
    () -> txtNombre.getText().trim().isEmpty() || txtEmail.getText().trim().isEmpty(),
    txtNombre.textProperty(),
    txtEmail.textProperty()
);
btnGuardar.disableProperty().bind(formularioInvalido);

// El botón eliminar se habilita solo si existe una selección activa en la tabla
btnEliminar.disableProperty().bind(
    tabla.getSelectionModel().selectedItemProperty().isNull()
);

```

---

### D. Concurrencia y Sincronización con APIs REST

#### Enfoque Imperativo (`Thread` + `Platform.runLater`)

```java
btnCargar.setDisable(true);
lblEstado.setText("Descargando...");

new Thread(() -> {
    try {
        List<Usuario> datos = apiService.obtenerUsuarios();
        // Hay que despachar manualmente la actualización al JavaFX Application Thread
        Platform.runLater(() -> {
            tabla.getItems().setAll(datos);
            btnCargar.setDisable(false);
            lblEstado.setText("Completado");
        });
    } catch (Exception ex) {
        Platform.runLater(() -> btnCargar.setDisable(false));
    }
}).start();

```

#### Enfoque Reactivo (`javafx.concurrent.Task` + Propiedades Enlazadas)

```java
Task<List<Usuario>> tareaDescarga = apiService.crearTareaDescarga();

// Enlace declarativo entre el ciclo de vida de la tarea y la interfaz
lblEstado.textProperty().bind(tareaDescarga.messageProperty());
indicadorProgreso.visibleProperty().bind(tareaDescarga.runningProperty());
btnCargar.disableProperty().bind(tareaDescarga.runningProperty());

// Callback reactivo en el JavaFX Application Thread
tareaDescarga.setOnSucceeded(e -> {
    listaUsuarios.setAll(tareaDescarga.getValue());
});

new Thread(tareaDescarga).start();

```

---

## 4. Criterios de Selección Técnica

* **Usar el enfoque imperativo cuando:**
* Se desarrollen herramientas de visualización de datos de solo lectura o dashboards analíticos estáticos.
* Se deba priorizar un bajo consumo de memoria RAM o procesar colecciones masivas de datos sin interacción individual celda por celda.


* **Usar el enfoque reactivo cuando:**
* La aplicación contenga formularios maestro-detalle con operaciones CRUD completas y edición interactiva en vivo.
* Existan reglas de validación complejas, cálculos dependientes en tiempo real o botones cuyo estado deba sincronizarse con la selección y entradas del usuario.