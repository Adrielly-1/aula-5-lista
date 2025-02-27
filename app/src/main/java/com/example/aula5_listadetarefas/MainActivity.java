package com.example.aula5_listadetarefas;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private EditText editTextTarefa;
    private Button botaoInserir;
    private ListView minhaListView;
    private ArrayList<String> itens;
    private ArrayList<String> datasHoras;
    private ArrayList<Integer> ids;
    private ArrayList<String> statusTarefas;
    private ArrayAdapter<String> adaptador;
    private TextView campoDataHora;
    private SQLiteDatabase bancoDeDados;
    private RadioGroup radioGroup;
    private int statusGeral = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextTarefa = findViewById(R.id.editTextTarefa);
        botaoInserir = findViewById(R.id.botaoInserir);
        minhaListView = findViewById(R.id.minhaListView);
        campoDataHora = findViewById(R.id.textviewDataHora);

        Button botaoExportar = findViewById(R.id.botaoExportar);
        botaoExportar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportarTarefasConcluidas();
            }
        });

        radioGroup = findViewById(R.id.radioGroupStatus);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radioPendente) {
                    statusGeral = 1;
                    carregarTarefas(1);
                } else if (checkedId == R.id.radioConcluida) {
                    statusGeral = 0;
                    carregarTarefas(0);
                }
            }
        });


        criarBancoDeDados();

        botaoInserir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String novaTarefa = editTextTarefa.getText().toString().trim();
                String dataHora = campoDataHora.getText().toString();

                if (novaTarefa.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Digite uma tarefa!",
                            Toast.LENGTH_SHORT).show();
                } else if (dataHora.equals("Data e Hora")) {
                    Toast.makeText(MainActivity.this, "Selecione a data e hora!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    adicionarTarefa(novaTarefa, dataHora);
                }
            }
        });

        minhaListView.setOnItemClickListener(((parent, view, position, id) -> {
            alternarStatusTarefa(position);
        }));

        minhaListView.setOnItemLongClickListener(((parent, view, position, id) -> {
            confirmarAcoes(position);
            return true;
        }));


        carregarTarefas(statusGeral);

    }

    private void exportarTarefasConcluidas() {
        try {
            File diretorio = new File(getExternalFilesDir(null), "Tarefas.txt");
            FileWriter writer = new FileWriter(diretorio);

            Cursor cursor = bancoDeDados.rawQuery("SELECT * FROM tarefas WHERE status = ?", new String[]{String.valueOf(statusGeral)});
            int indiceTarefa = cursor.getColumnIndex("tarefa");
            int indiceDataHora = cursor.getColumnIndex("dataHora");

            String titulo = statusGeral == 1 ? "Pendentes" : "Concluidas";
            if (cursor.moveToFirst()) {
                writer.write("Tarefas " + titulo + ": \n\n");
                do {
                    String tarefa = cursor.getString(indiceTarefa);
                    String dataHora = cursor.getString(indiceDataHora);
                    writer.write("Tarefa: " + tarefa + "\nData e Hora: " + dataHora + "\n\n");
                } while (cursor.moveToNext());
            }

            cursor.close();
            writer.close();

            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", diretorio);
            Intent intent = new Intent(Intent.ACTION_SEND);

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_SUBJECT, "Tarefas " + titulo);
            startActivity(Intent.createChooser(intent, "Compartilhar via"));

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao exportar tarefas", Toast.LENGTH_SHORT).show();
        }
    }


    public void criarBancoDeDados() {
        try {
            bancoDeDados = openOrCreateDatabase("ListaTarefasApp", MODE_PRIVATE, null);
            bancoDeDados.execSQL("CREATE TABLE IF NOT EXISTS " +
                    "tarefas (id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "tarefa VARCHAR, dataHora VARCHAR, status VARCHAR)");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void adicionarTarefa(String tarefa, String dataHora) {
        try {
            String sql = "INSERT INTO tarefas (tarefa, dataHora, status) VALUES (?, ?, ?)";
            SQLiteStatement stmt = bancoDeDados.compileStatement(sql);
            stmt.bindString(1, tarefa);
            stmt.bindString(2, dataHora);
            stmt.bindString(3, "1");
            stmt.executeInsert();

            editTextTarefa.setText("");
            campoDataHora.setText("Data e Hora");
            Toast.makeText(MainActivity.this, "Tarefa adicionada com sucesso!",
                    Toast.LENGTH_SHORT).show();

            carregarTarefas(statusGeral);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void carregarTarefas(int status) {
        try {
            Cursor cursor = bancoDeDados.rawQuery("SELECT * FROM tarefas WHERE status = ? ORDER BY id DESC",
                    new String[]{String.valueOf(status)});
            int indiceId = cursor.getColumnIndex("id");
            int indiceTarefa = cursor.getColumnIndex("tarefa");
            int indiceDataHora = cursor.getColumnIndex("dataHora");
            int indiceStatus = cursor.getColumnIndex("status");

            ids = new ArrayList<>();
            itens = new ArrayList<>();
            datasHoras = new ArrayList<>();
            statusTarefas = new ArrayList<>();

            adaptador = new ArrayAdapter<String>(this, R.layout.linhacustomizada, R.id.texto1, itens) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    TextView texto1 = view.findViewById(R.id.texto1);
                    TextView texto2 = view.findViewById(R.id.texto2);

                    texto1.setText(itens.get(position));
                    texto2.setText(datasHoras.get(position));

                    if (statusTarefas.get(position).equals("1")) {
                        texto1.setTextColor(Color.BLACK);
                        texto2.setTextColor(Color.BLUE);
                        texto1.setPaintFlags(texto1.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                        texto2.setPaintFlags(texto2.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    } else {
                        texto1.setTextColor(Color.GRAY);
                        texto2.setTextColor(Color.GRAY);
                        texto1.setPaintFlags(texto1.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                        texto2.setPaintFlags(texto2.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    }

                    return view;
                }
            };
            minhaListView.setAdapter(adaptador);

            cursor.moveToFirst();
            while (cursor != null) {
                ids.add(cursor.getInt(indiceId));
                itens.add(cursor.getString(indiceTarefa));
                datasHoras.add(cursor.getString(indiceDataHora));
                statusTarefas.add(cursor.getString(indiceStatus));
                cursor.moveToNext();
            }
            cursor.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void excluirTarefa(int position) {
        try {
            bancoDeDados.execSQL("DELETE FROM tarefas WHERE id = " + ids.get(position));
            Toast.makeText(MainActivity.this, "Tarefa excluída com sucesso!",
                    Toast.LENGTH_SHORT).show();
            carregarTarefas(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void confirmarExclusao(int position) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Confirmação")
                .setMessage("Deseja realmente apagar a tarefa \"" + itens.get(position) + "\"?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    excluirTarefa(position);
                })
                .setNegativeButton("Não", null)
                .show();
    }

    private void confirmarAcoes(int position) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Escolha uma ação")
                .setMessage("O que você deseja fazer com a tarefa \"" + itens.get(position) + "\"?")
                .setPositiveButton("Editar Tarefa", (dialog, which) -> {
                    editarTarefa(position); // Chama o método para editar a tarefa
                })
                .setNegativeButton("Apagar Tarefa", (dialog, which) -> {
                    confirmarExclusao(position); // Chama o método para excluir a tarefa
                })
                .setNeutralButton("Cancelar", null) // Para cancelar
                .show();
    }

    private void editarTarefa(int position) {
        final EditText editTarefa = new EditText(MainActivity.this);
        editTarefa.setText(itens.get(position));

        campoDataHora.setText(datasHoras.get(position));

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Editar Tarefa")
                .setMessage("Edite o título e a data/hora da tarefa.")
                .setView(editTarefa)
                .setPositiveButton("Escolher Data/Hora", (dialog, which) -> {
                    AlertDialog alertDialog = (AlertDialog) dialog;
                    _pegarDataHoraEditar(alertDialog);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .setNeutralButton("Salvar", (dialog, which) -> {
                    String novoNome = editTarefa.getText().toString().trim();
                    String dataHora = campoDataHora.getText().toString();

                    String toDatahora = null;

                    if (dataHora != "Data e Hora") {
                        toDatahora = dataHora;
                    }

                    if (!novoNome.isEmpty() && !dataHora.isEmpty()) {
                        atualizarTarefa(position, novoNome, toDatahora);
                    } else {
                        Toast.makeText(MainActivity.this, "Por favor, preencha todos os campos.",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        campoDataHora.setText(datasHoras.get(position));
    }

    public void _pegarDataHoraCriar(View view) {
        Calendar calendario = Calendar.getInstance();
        int dia = calendario.get(Calendar.DAY_OF_MONTH);
        int mes = calendario.get(Calendar.MONTH);
        int ano = calendario.get(Calendar.YEAR);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view1, year, month, dayOfMonth) -> {
                    String dataSelecionada = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    campoDataHora.setText(dataSelecionada);
                    _pegarHoraCriar(null);
                }, ano, mes, dia);
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    public void _pegarDataHoraEditar(AlertDialog parentDialog) {
        Calendar calendario = Calendar.getInstance();
        int dia = calendario.get(Calendar.DAY_OF_MONTH);
        int mes = calendario.get(Calendar.MONTH);
        int ano = calendario.get(Calendar.YEAR);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view1, year, month, dayOfMonth) -> {
                    String dataSelecionada = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    campoDataHora.setText(dataSelecionada);

                    _pegarHoraEditar(year, month, dayOfMonth, parentDialog);
                }, ano, mes, dia);
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    public void _pegarHoraEditar(int ano, int mes, int dia, AlertDialog parentDialog) {
        Calendar calendario = Calendar.getInstance();
        int hora = calendario.get(Calendar.HOUR_OF_DAY);
        int minuto = calendario.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    String horaSelecionada = String.format("%02d:%02d", hourOfDay, minute);

                    String dataHoraCompleta = String.format("%02d/%02d/%04d %s", dia, mes + 1, ano, horaSelecionada);
                    campoDataHora.setText(dataHoraCompleta);

                    parentDialog.show();
                }, hora, minuto, true);
        timePickerDialog.show();
    }

    private void _pegarHoraCriar(View view) {
        Calendar calendario = Calendar.getInstance();
        int hora = calendario.get(Calendar.HOUR_OF_DAY);
        int minuto = calendario.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view12, hourOfDay, minute) -> {

                    String time = String.format("%02d:%02d", hourOfDay, minute);
                    campoDataHora.setText(campoDataHora.getText().toString() + " " + time);

                }, hora, minuto, true);
        timePickerDialog.show();
    }


    private void atualizarTarefa(int position, String novoNome, String novaDataHora) {
        try {
            String sql = "UPDATE tarefas SET tarefa = ?, dataHora = ? WHERE id = ?";
            SQLiteStatement stmt = bancoDeDados.compileStatement(sql);
            stmt.bindString(1, novoNome);

            if (novaDataHora != null && !novaDataHora.isEmpty()) {
                String data = novaDataHora;
                stmt.bindString(2, data);
                campoDataHora.setText(data);
            } else {
                stmt.bindNull(2);
            }

            stmt.bindLong(3, ids.get(position));
            stmt.executeUpdateDelete();

            carregarTarefas(statusGeral);
            Toast.makeText(MainActivity.this, "Tarefa atualizada com sucesso!", Toast.LENGTH_SHORT).show();
            campoDataHora.setText("Data e Hora");
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Erro ao atualizar a tarefa.", Toast.LENGTH_SHORT).show();
        }
    }


    private void alternarStatusTarefa(int position) {
        try {
            String novoStatus = statusTarefas.get(position).equals("1") ? "0" : "1";
            bancoDeDados.execSQL("UPDATE tarefas SET status = '" +
                    novoStatus + "' WHERE id = " + ids.get(position));

            carregarTarefas(statusGeral);
            if (novoStatus.equals("1")) {
                Toast.makeText(MainActivity.this, "Tarefa ativa!",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Tarefa concluída!",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}