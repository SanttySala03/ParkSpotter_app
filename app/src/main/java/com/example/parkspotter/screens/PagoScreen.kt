package com.example.parkspotter.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parkspotter.model.EstadoPago
import com.example.parkspotter.model.Pago
import com.example.parkspotter.ui.components.BotonPrimario
import com.example.parkspotter.ui.components.EstadoVacio
import com.example.parkspotter.ui.components.camposColores
import com.example.parkspotter.ui.theme.*
import com.example.parkspotter.util.Formatos
import com.example.parkspotter.util.Tarjeta
import com.example.parkspotter.viewmodel.PagoViewModel
import java.time.ZonedDateTime

/** Sprint 5 — Pantalla de pago de una reserva con tarjeta (tokenizada en el celular). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagoScreen(reservaId: String, onVolver: () -> Unit, onAprobado: (String) -> Unit) {
    val viewModel: PagoViewModel = viewModel(factory = PagoViewModel.Factory)
    val estado by viewModel.estado.collectAsState()
    val ahora = viewModel.ahora()

    LaunchedEffect(reservaId) { viewModel.cargarReserva(reservaId) }
    LaunchedEffect(estado.pago?.estado) {
        if (estado.pago?.estado == EstadoPago.APPROVED) onAprobado(reservaId)
    }

    IconosBarraEstado(fondoOscuro = true)

    Scaffold(
        containerColor = BackgroundGray,
        topBar = {
            TopAppBar(
                title = { Text("Pagar reserva", color = SurfaceWhite, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onVolver, enabled = !estado.procesando) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = SurfaceWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        }
    ) { padding ->
        val reserva = estado.reserva
        when {
            estado.cargando -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator(color = BluePrimary) }
            reserva == null -> EstadoVacio(
                icono = Icons.Filled.ErrorOutline,
                titulo = "No pudimos cargar la reserva",
                texto = estado.error ?: "Intenta de nuevo",
                accion = "Reintentar",
                onAccion = { viewModel.cargarReserva(reservaId) },
                modifier = Modifier.padding(padding)
            )
            else -> Column(Modifier.fillMaxSize().padding(padding)) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).imePadding().padding(16.dp)) {
                    // Resumen de lo que se paga (el monto lo fija el servidor según la reserva)
                    Surface(shape = RoundedCornerShape(18.dp), color = BluePrimary, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp)) {
                            Text(reserva.garajeNombre, color = SurfaceWhite, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(Formatos.intervalo(reserva.inicio, reserva.fin, ahora), color = SurfaceWhite.copy(alpha = 0.85f), fontSize = 13.sp)
                            Text("Reserva ${reserva.codigo}", color = SurfaceWhite.copy(alpha = 0.85f), fontSize = 13.sp)
                            Spacer(Modifier.height(10.dp))
                            Text("Total a pagar", color = SurfaceWhite.copy(alpha = 0.8f), fontSize = 12.sp)
                            Text(Formatos.pesos(reserva.total), color = GreenAccent, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Text("Tarjeta de crédito o débito", fontWeight = FontWeight.SemiBold, color = TextDark,
                        modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))
                    CampoTarjeta(estado.numero, viewModel::cambiarNumero, "Número de la tarjeta", "4242 4242 4242 4242", KeyboardType.Number,
                        trailing = Tarjeta.franquicia(estado.numero))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CampoTarjeta(estado.vence, viewModel::cambiarVence, "Vence (MM/AA)", "12/29", KeyboardType.Number, Modifier.weight(1f))
                        CampoTarjeta(estado.cvv, viewModel::cambiarCvv, "CVV", "123", KeyboardType.NumberPassword, Modifier.weight(1f), oculto = true)
                    }
                    CampoTarjeta(estado.titular, viewModel::cambiarTitular, "Nombre en la tarjeta", "Como aparece impreso", KeyboardType.Text,
                        mayusculas = true)

                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Blue50).padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Filled.Lock, null, tint = BluePrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Tus datos de tarjeta no se guardan: se convierten en un código seguro antes de enviarse. " +
                                "Pruebas: ${Tarjeta.PRUEBA_APROBADA} (aprobada) · ${Tarjeta.PRUEBA_RECHAZADA} (rechazada).",
                            fontSize = 12.sp, color = TextDark
                        )
                    }
                    estado.error?.let {
                        Text(it, color = RedError, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
                    }
                }
                Surface(color = SurfaceWhite, shadowElevation = 12.dp) {
                    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                        if (estado.procesando) {
                            Text("Procesando el pago con el banco…", fontSize = 13.sp, color = TextMedium,
                                modifier = Modifier.padding(bottom = 8.dp))
                        }
                        BotonPrimario(
                            texto = "Pagar ${Formatos.pesos(reserva.total)}",
                            onClick = viewModel::pagar,
                            cargando = estado.procesando,
                            acento = true,
                            icono = Icons.Filled.CreditCard,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CampoTarjeta(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    ejemplo: String,
    teclado: KeyboardType,
    modifier: Modifier = Modifier.fillMaxWidth(),
    trailing: String? = null,
    oculto: Boolean = false,
    mayusculas: Boolean = false
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        placeholder = { Text(ejemplo) },
        singleLine = true,
        trailingIcon = trailing?.let { { Text(it, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BluePrimary, modifier = Modifier.padding(end = 12.dp)) } },
        visualTransformation = if (oculto) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = teclado,
            capitalization = if (mayusculas) KeyboardCapitalization.Characters else KeyboardCapitalization.None
        ),
        shape = RoundedCornerShape(12.dp),
        colors = camposColores(),
        modifier = modifier.padding(bottom = 12.dp)
    )
}

/** Sprint 5 — Comprobante de pago de una reserva (se puede compartir). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComprobanteScreen(reservaId: String, onVolver: () -> Unit) {
    val viewModel: PagoViewModel = viewModel(factory = PagoViewModel.Factory)
    val estado by viewModel.estado.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(reservaId) { viewModel.cargarComprobante(reservaId) }
    IconosBarraEstado(fondoOscuro = true)

    Scaffold(
        containerColor = BackgroundGray,
        topBar = {
            TopAppBar(
                title = { Text("Comprobante", color = SurfaceWhite, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = SurfaceWhite) }
                },
                actions = {
                    estado.pago?.let { pago ->
                        IconButton(onClick = {
                            val envio = Intent(Intent.ACTION_SEND).setType("text/plain")
                                .putExtra(Intent.EXTRA_TEXT, textoComprobante(pago, viewModel.ahora()))
                            context.startActivity(Intent.createChooser(envio, "Compartir comprobante"))
                        }) { Icon(Icons.Filled.Share, "Compartir comprobante", tint = SurfaceWhite) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        }
    ) { padding ->
        val pago = estado.pago
        when {
            estado.cargando -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator(color = BluePrimary) }
            pago == null -> EstadoVacio(
                icono = Icons.Filled.ReceiptLong,
                titulo = "Sin comprobante",
                texto = estado.error ?: "No encontramos el pago",
                modifier = Modifier.padding(padding)
            )
            else -> Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val aprobado = pago.estado == EstadoPago.APPROVED
                Box(Modifier.size(80.dp).clip(CircleShape).background(if (aprobado) Green50 else Blue50), contentAlignment = Alignment.Center) {
                    Icon(if (aprobado) Icons.Filled.CheckCircle else Icons.Filled.ReceiptLong, null,
                        tint = if (aprobado) GreenAccentDark else BluePrimary, modifier = Modifier.size(48.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(if (aprobado) "Pago aprobado" else pago.estado.etiqueta, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text(Formatos.pesos(pago.monto), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = BluePrimary)
                Spacer(Modifier.height(16.dp))
                Surface(shape = RoundedCornerShape(18.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        FilaComprobante("Comprobante", pago.comprobante ?: "—")
                        FilaComprobante("Reserva", pago.codigoReserva)
                        FilaComprobante("Garaje", pago.garajeNombre)
                        if (pago.garajeDireccion.isNotBlank()) FilaComprobante("Dirección", pago.garajeDireccion)
                        if (pago.inicio != null && pago.fin != null) FilaComprobante("Horario", Formatos.intervalo(pago.inicio, pago.fin, viewModel.ahora()))
                        pago.placa?.let { FilaComprobante("Placa", it) }
                        FilaComprobante("Tarjeta", "${pago.franquicia} •••• ${pago.ultimos4}")
                        pago.aprobadoEn?.let { FilaComprobante("Fecha de pago", "${Formatos.fecha(it)}, ${Formatos.hora(it)}") }
                        pago.transaccion?.let { FilaComprobante("Transacción", it) }
                        FilaComprobante("Referencia", pago.referencia)
                        pago.reembolsadoEn?.let { FilaComprobante("Reembolso", "${Formatos.fecha(it)}, ${Formatos.hora(it)}") }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("ParkSpotter · Encuentra tu espacio", fontSize = 12.sp, color = TextMedium, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun FilaComprobante(etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(etiqueta, fontSize = 13.sp, color = TextMedium, modifier = Modifier.width(110.dp))
        Text(valor, fontSize = 13.sp, color = TextDark, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}

private fun textoComprobante(p: Pago, ahora: ZonedDateTime): String = buildString {
    appendLine("Comprobante de pago ParkSpotter")
    appendLine("Comprobante: ${p.comprobante ?: "—"}")
    appendLine("Estado: ${p.estado.etiqueta}")
    appendLine("Valor: ${Formatos.pesos(p.monto)}")
    appendLine("Reserva: ${p.codigoReserva} · ${p.garajeNombre}")
    if (p.inicio != null && p.fin != null) appendLine("Horario: ${Formatos.intervalo(p.inicio, p.fin, ahora)}")
    appendLine("Tarjeta: ${p.franquicia} •••• ${p.ultimos4}")
    append("Referencia: ${p.referencia}")
}
