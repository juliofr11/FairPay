package es.ifp.fairpay.data.database;

import android.util.Log;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseConnection {

    private static final String URL = "jdbc:mysql://mysql-fairpay-fairpay-bd.i.aivencloud.com:11587/fairpay_db?ssl-mode=REQUIRED";
    private static final String USER = "avnadmin";
    private static final String PASSWORD = "AVNS_06D48ZfjtfPIUDBlV0P";

    // --- INTERFACES PARA CALLBACKS ---
    // Estas interfaces permiten manejar las respuestas asíncronas de la base de datos

    public interface ResultSetProcessor {
        void process(ResultSet rs) throws SQLException;
    }

    public interface LoginListener {
        void onLoginSuccess(String nombre, String apellidos, String privateKey);
        void onLoginFailure(String error);
    }

    public interface RegistroListener {
        void onRegistroSuccess(String nombre, String privateKey);
        void onRegistroFailure(String error);
    }

    public interface SaldoListener {
        void onSaldoObtenido(double saldo);
        void onError(String error);
    }

    public interface OperacionListener {
        void onOperacionExito(String mensaje);
        void onOperacionFallo(String error);
    }

    public interface DataListener<T> {
        void onDataSuccess(List<T> data);
        void onDataFailure(String error);
    }

    // --- MÉTODOS PRINCIPALES ---

    // Esta función se encarga de registrar un nuevo usuario en la base de datos, guardando sus datos personales y creando su billetera asociada
    public void registrarUsuario(RegistroListener listener, String nombre, String apellidos, String correo, String contrasena_hash, String telefono, String direccionBilletera, String clavePrivadaCifrada) {

        if (nombre == null || nombre.isEmpty() || apellidos == null || apellidos.isEmpty() || correo == null || correo.isEmpty() || contrasena_hash == null || contrasena_hash.isEmpty() || direccionBilletera == null || direccionBilletera.isEmpty()) {
            listener.onRegistroFailure("Todos los campos son obligatorios.");
            return;
        }

        new Thread(() -> {
            Connection conn = null;
            PreparedStatement pstmtUsuario = null;
            PreparedStatement pstmtBilletera = null;
            PreparedStatement pstmtEmail = null;
            ResultSet rsEmail = null;
            ResultSet generatedKeys = null;

            try {
                conn = getCompatibleConnection();
                conn.setAutoCommit(false);

                // Verificar si el correo ya existe
                String sqlEmail = "SELECT correo FROM Usuario WHERE correo = ?";
                pstmtEmail = conn.prepareStatement(sqlEmail);
                pstmtEmail.setString(1, correo);
                rsEmail = pstmtEmail.executeQuery();
                if (rsEmail.next()) {
                    throw new SQLException("El correo electrónico ya está en uso.");
                }

                // Insertar datos del Usuario
                String sqlUsuario = "INSERT INTO Usuario (nombre, apellidos, correo, contraseña_hash, telefono) VALUES (?, ?, ?, ?, ?)";
                pstmtUsuario = conn.prepareStatement(sqlUsuario, Statement.RETURN_GENERATED_KEYS);
                pstmtUsuario.setString(1, nombre);
                pstmtUsuario.setString(2, apellidos);
                pstmtUsuario.setString(3, correo);
                pstmtUsuario.setString(4, contrasena_hash);
                pstmtUsuario.setString(5, telefono);

                int rowsAffected = pstmtUsuario.executeUpdate();
                if (rowsAffected == 0) {
                    throw new SQLException("Fallo al insertar usuario.");
                }

                // Recuperar el ID generado para vincular la billetera
                generatedKeys = pstmtUsuario.getGeneratedKeys();
                long idUsuario;
                if (generatedKeys.next()) {
                    idUsuario = generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Fallo al obtener el ID del usuario.");
                }

                // Insertar la Billetera vinculada
                String sqlBilletera = "INSERT INTO Billetera (dirección, clave_privada_cifrada, id_usuario) VALUES (?, ?, ?)";
                pstmtBilletera = conn.prepareStatement(sqlBilletera);
                pstmtBilletera.setString(1, direccionBilletera);
                pstmtBilletera.setString(2, clavePrivadaCifrada);
                pstmtBilletera.setLong(3, idUsuario);
                pstmtBilletera.executeUpdate();

                conn.commit();
                Log.d("FairPayDB", "Registro exitoso: " + nombre);
                listener.onRegistroSuccess(nombre, clavePrivadaCifrada);

            } catch (Throwable e) {
                Log.e("FairPayDB", "Error en registro", e);
                rollbackQuietly(conn);
                listener.onRegistroFailure("Error: " + e.getMessage());

            } finally {
                closeQuietly(rsEmail);
                closeQuietly(generatedKeys);
                closeQuietly(pstmtEmail);
                closeQuietly(pstmtUsuario);
                closeQuietly(pstmtBilletera);
                closeQuietly(conn);
            }
        }).start();
    }

    // Esta función se encarga de verificar las credenciales del usuario (correo y contraseña) para permitir el acceso a la aplicación
    public void loginUsuario(LoginListener listener, String correo, String password){
        new Thread(() -> {
            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            try {
                conn = getCompatibleConnection();

                String sql = "SELECT u.nombre, u.apellidos, u.contraseña_hash, b.clave_privada_cifrada " +
                        "FROM Usuario u " +
                        "INNER JOIN Billetera b ON u.id_usuario = b.id_usuario " +
                        "WHERE u.correo = ?";

                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, correo);
                rs = pstmt.executeQuery();

                if (rs.next()){
                    String hashGuardado = rs.getString("contraseña_hash");
                    String nombreRecuperado = rs.getString("nombre");
                    String apellidosRecuperados = rs.getString("apellidos");
                    String pkRecuperada = rs.getString("clave_privada_cifrada");

                    if (pkRecuperada == null || pkRecuperada.trim().isEmpty()) {
                        listener.onLoginFailure("Error de integridad: Usuario sin billetera válida.");
                        return;
                    }

                    try {
                        if(BCrypt.checkpw(password, hashGuardado)){
                            listener.onLoginSuccess(nombreRecuperado, apellidosRecuperados, pkRecuperada);
                        } else {
                            listener.onLoginFailure("Contraseña incorrecta");
                        }
                    } catch (IllegalArgumentException e) {
                        listener.onLoginFailure("Error: Contraseña corrupta.");
                    }
                } else {
                    listener.onLoginFailure("Usuario no encontrado.");
                }
            } catch (Throwable e) {
                Log.e("FairPayDB", "Error en Login", e);
                handleError(e, listener::onLoginFailure);
            } finally {
                closeQuietly(rs);
                closeQuietly(pstmt);
                closeQuietly(conn);
            }
        }).start();
    }

    // --- MÉTODOS DE DISPUTAS ---

    // Esta función se encarga de registrar una nueva disputa en la base de datos vinculando al comprador y al vendedor con el depósito
    public void abrirDisputa(String correoUsuario, String escrowId, String rol, String motivo, String walletComprador, String walletVendedor, OperacionListener listener) {
        new Thread(() -> {
            Connection conn = null;
            PreparedStatement pstmt = null;
            try {
                conn = getCompatibleConnection();
                conn.setAutoCommit(false); // Iniciar transacción para asegurar consistencia

                long idComprador = obtenerIdUsuarioPorBilletera(conn, walletComprador);
                long idVendedor = obtenerIdUsuarioPorBilletera(conn, walletVendedor);

                String sql = "INSERT INTO Disputa (id_escrow, id_usuario, rol_usuario, motivo, estado_resolucion) VALUES (?, ?, ?, ?, 'ABIERTA')";
                pstmt = conn.prepareStatement(sql);

                boolean registered = false;

                // Insertar disputa vinculada al Comprador
                if (idComprador != -1) {
                    pstmt.setLong(1, Long.parseLong(escrowId));
                    pstmt.setLong(2, idComprador);
                    pstmt.setString(3, rol);
                    pstmt.setString(4, motivo);
                    pstmt.addBatch();
                    registered = true;
                }

                // Insertar disputa vinculada al Vendedor (si es distinto usuario)
                if (idVendedor != -1 && idVendedor != idComprador) {
                    pstmt.setLong(1, Long.parseLong(escrowId));
                    pstmt.setLong(2, idVendedor);
                    pstmt.setString(3, rol);
                    pstmt.setString(4, motivo);
                    pstmt.addBatch();
                    registered = true;
                }

                if (registered) {
                    pstmt.executeBatch();
                    conn.commit();
                    listener.onOperacionExito("Mensaje registrado para ambas partes.");
                } else {
                    conn.rollback();
                    // Fallback: Si no encuentra las wallets, intenta registrarlo solo para el usuario actual (correo)
                    long idSender = obtenerIdUsuarioPorCorreo(conn, correoUsuario);
                    if (idSender != -1) {
                        PreparedStatement pstmtBackup = conn.prepareStatement(sql);
                        pstmtBackup.setLong(1, Long.parseLong(escrowId));
                        pstmtBackup.setLong(2, idSender);
                        pstmtBackup.setString(3, rol);
                        pstmtBackup.setString(4, motivo);
                        pstmtBackup.executeUpdate();
                        conn.commit();
                        pstmtBackup.close();
                        listener.onOperacionExito("Registrado solo para ti (Contraparte no encontrada).");
                    } else {
                        listener.onOperacionFallo("Error: Usuarios no encontrados en BD.");
                    }
                }

            } catch (Throwable e) {
                rollbackQuietly(conn);
                Log.e("FairPayDB", "Error al abrir disputa", e);
                listener.onOperacionFallo("Error al registrar disputa: " + e.getMessage());
            } finally {
                closeQuietly(pstmt);
                closeQuietly(conn);
            }
        }).start();
    }

    // Esta función se encarga de buscar el ID interno de usuario a partir de su dirección de billetera pública
    private long obtenerIdUsuarioPorBilletera(Connection conn, String wallet) throws SQLException {
        PreparedStatement ps = null; ResultSet rs = null;
        try {
            String sql = "SELECT id_usuario FROM Billetera WHERE dirección = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, wallet);
            rs = ps.executeQuery();
            if (rs.next()) return rs.getLong("id_usuario");
            return -1;
        } finally { closeQuietly(rs); closeQuietly(ps); }
    }

    // Esta función se encarga de recuperar la lista de disputas activas o en revisión asociadas al usuario o al administrador
    public void obtenerDisputas(String correoUsuario, DataListener<Map<String, String>> listener) {
        new Thread(() -> {
            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            List<Map<String, String>> disputas = new ArrayList<>();
            try {
                conn = getCompatibleConnection();
                long idUsuarioActual = obtenerIdUsuarioPorCorreo(conn, correoUsuario);

                String sql;
                if (correoUsuario.equalsIgnoreCase("firewire80a@gmail.com")) {
                    // Si es la Plataforma (Admin), recupera todas las disputas abiertas o en revisión
                    sql = "SELECT d.id_escrow, d.rol_usuario, d.motivo, d.fecha_creacion, d.estado_resolucion, d.decision_final " +
                            "FROM Disputa d " +
                            "WHERE d.estado_resolucion IN ('ABIERTA', 'REVISION') " +
                            "GROUP BY d.id_escrow, d.rol_usuario, d.motivo, d.fecha_creacion, d.estado_resolucion, d.decision_final " +
                            "ORDER BY d.fecha_creacion DESC";
                    pstmt = conn.prepareStatement(sql);
                } else {
                    // Si es un usuario normal, recupera solo sus disputas
                    sql = "SELECT id_escrow, rol_usuario, motivo, fecha_creacion, estado_resolucion, decision_final " +
                            "FROM Disputa WHERE id_usuario = ?";
                    pstmt = conn.prepareStatement(sql);
                    pstmt.setLong(1, idUsuarioActual);
                }

                if (pstmt != null) {
                    rs = pstmt.executeQuery();
                    while (rs.next()) {
                        Map<String, String> disputa = new HashMap<>();
                        disputa.put("escrowId", String.valueOf(rs.getLong("id_escrow")));
                        disputa.put("rol", rs.getString("rol_usuario"));
                        disputa.put("motivo", rs.getString("motivo"));
                        disputa.put("fecha", rs.getString("fecha_creacion"));
                        disputa.put("estado", rs.getString("estado_resolucion"));
                        disputa.put("decision", rs.getString("decision_final"));

                        disputas.add(disputa);
                    }
                }
                listener.onDataSuccess(disputas);

            } catch (Throwable e) {
                Log.e("FairPayDB", "Error al obtener disputas", e);
                listener.onDataFailure("Error al obtener disputas: " + e.getMessage());
            } finally {
                closeQuietly(rs);
                closeQuietly(pstmt);
                closeQuietly(conn);
            }
        }).start();
    }

    // Esta función se encarga de actualizar el estado de una disputa a "REVISION" para que la plataforma intervenga
    public void solicitarRevision(String escrowId, OperacionListener listener) {
        new Thread(() -> {
            Connection conn = null;
            PreparedStatement pstmt = null;
            try {
                conn = getCompatibleConnection();
                String sql = "UPDATE Disputa SET estado_resolucion = 'REVISION' WHERE id_escrow = ? AND estado_resolucion = 'ABIERTA'";
                pstmt = conn.prepareStatement(sql);
                pstmt.setLong(1, Long.parseLong(escrowId));

                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    listener.onOperacionExito("Solicitud de revisión enviada. Chat cerrado.");
                } else {
                    listener.onOperacionFallo("No se pudo actualizar. Quizás ya esté cerrada o en revisión.");
                }
            } catch (Throwable e) {
                Log.e("FairPayDB", "Error solicitar revision", e);
                listener.onOperacionFallo("Error BD: " + e.getMessage());
            } finally {
                closeQuietly(pstmt);
                closeQuietly(conn);
            }
        }).start();
    }

    // Esta función se encarga de finalizar una disputa estableciendo una decisión final y cambiando su estado a "RESUELTA"
    public void marcarDisputaResuelta(String escrowId, String decisionFinal, OperacionListener listener) {
        new Thread(() -> {
            Connection conn = null;
            PreparedStatement pstmt = null;
            try {
                conn = getCompatibleConnection();
                String sql = "UPDATE Disputa SET estado_resolucion = 'RESUELTA', decision_final = ? WHERE id_escrow = ?";
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, decisionFinal);
                pstmt.setLong(2, Long.parseLong(escrowId));
                int rows = pstmt.executeUpdate();
                if (rows > 0) listener.onOperacionExito("Disputa ID " + escrowId + " marcada como RESUELTA.");
                else listener.onOperacionFallo("No se encontró la disputa ID " + escrowId + " para actualizar.");
            } catch (Throwable e) {
                Log.e("FairPayDB", "Error al marcar disputa resuelta", e);
                listener.onOperacionFallo("Error al resolver disputa: " + e.getMessage());
            } finally { closeQuietly(pstmt); closeQuietly(conn); }
        }).start();
    }

    // Esta función se encarga de consultar el saldo actual disponible en la billetera del usuario
    public void obtenerSaldo(String correo, SaldoListener listener) {
        new Thread(() -> {
            Connection conn = null; PreparedStatement pstmt = null; ResultSet rs = null;
            try {
                conn = getCompatibleConnection();
                String sql = "SELECT b.saldo FROM Billetera b JOIN Usuario u ON b.id_usuario = u.id_usuario WHERE u.correo = ?";
                pstmt = conn.prepareStatement(sql); pstmt.setString(1, correo); rs = pstmt.executeQuery();
                if (rs.next()) listener.onSaldoObtenido(rs.getDouble("saldo"));
                else listener.onError("Billetera no encontrada.");
            } catch (Throwable e) { Log.e("FairPayDB", "Error saldo", e); listener.onError("Error: " + e.getMessage()); }
            finally { closeQuietly(rs); closeQuietly(pstmt); closeQuietly(conn); }
        }).start();
    }

    // Esta función se encarga de simular una recarga de saldo en la billetera del usuario (útil para pruebas)
    public void crearDeposito(String correo, double cantidad, OperacionListener listener) {
        new Thread(() -> {
            Connection conn = null; PreparedStatement pstmt = null;
            try {
                conn = getCompatibleConnection(); conn.setAutoCommit(false);
                long id = obtenerIdUsuarioPorCorreo(conn, correo);
                if (id == -1) throw new SQLException("Usuario no encontrado");
                try {
                    String sql = "UPDATE Billetera SET saldo = saldo + ? WHERE id_usuario = ?";
                    pstmt = conn.prepareStatement(sql); pstmt.setDouble(1, cantidad); pstmt.setLong(2, id);
                    pstmt.executeUpdate();
                } catch(Exception e) {}
                conn.commit(); listener.onOperacionExito("Depósito simulado.");
            } catch (Throwable e) { rollbackQuietly(conn); listener.onOperacionFallo("Error: " + e.getMessage()); }
            finally { closeQuietly(pstmt); closeQuietly(conn); }
        }).start();
    }

    // Esta función se encarga de recuperar las últimas transacciones realizadas por el usuario para mostrarlas en su historial
    public void obtenerHistorialOperaciones(String correo, DataListener<Map<String, String>> listener) {
        new Thread(() -> {
            Connection conn = null; PreparedStatement pstmt = null; ResultSet rs = null;
            List<Map<String, String>> lista = new ArrayList<>();
            try {
                conn = getCompatibleConnection();
                long id = obtenerIdUsuarioPorCorreo(conn, correo);
                String sql = "SELECT * FROM Transaccion WHERE id_usuario = ? ORDER BY fecha DESC LIMIT 20";
                try {
                    pstmt = conn.prepareStatement(sql); pstmt.setLong(1, id); rs = pstmt.executeQuery();
                    while (rs.next()) {
                        Map<String, String> op = new HashMap<>();
                        op.put("tipo", rs.getString("tipo")); op.put("cantidad", String.valueOf(rs.getDouble("cantidad"))); op.put("fecha", rs.getString("fecha"));
                        lista.add(op);
                    }
                    listener.onDataSuccess(lista);
                } catch(Exception e) { listener.onDataSuccess(new ArrayList<>()); }
            } catch (Throwable e) { listener.onDataFailure(e.getMessage()); }
            finally { closeQuietly(rs); closeQuietly(pstmt); closeQuietly(conn); }
        }).start();
    }

    // --- MÉTODOS DE UTILIDAD ---

    // Esta función se encarga de establecer la conexión técnica con el servidor de base de datos MySQL
    private Connection getCompatibleConnection() throws SQLException, ClassNotFoundException {
        try { Class.forName("com.mysql.jdbc.Driver"); } catch (ClassNotFoundException e) { Class.forName("com.mysql.cj.jdbc.Driver"); }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Esta función se encarga de obtener el ID interno del usuario a partir de su correo electrónico
    private long obtenerIdUsuarioPorCorreo(Connection conn, String correo) throws SQLException {
        PreparedStatement ps = null; ResultSet rs = null;
        try { String sql = "SELECT id_usuario FROM Usuario WHERE correo = ?"; ps = conn.prepareStatement(sql); ps.setString(1, correo); rs = ps.executeQuery(); if (rs.next()) return rs.getLong("id_usuario"); return -1; } finally { closeQuietly(rs); closeQuietly(ps); }
    }

    // Esta función se encarga de revertir los cambios en la base de datos si ocurre un error durante una transacción
    private void rollbackQuietly(Connection conn) { try { if (conn != null) conn.rollback(); } catch (SQLException e) { Log.e("FairPayDB", "Error rollback", e); } }

    // Esta función se encarga de cerrar los recursos de la base de datos de forma segura para liberar memoria
    private void closeQuietly(AutoCloseable r) { try { if (r != null) r.close(); } catch (Exception e) {} }

    private interface ErrorCallback { void call(String msg); }
    private void handleError(Throwable e, ErrorCallback c) { c.call(e.getMessage()); }
}