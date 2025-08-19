# Script Python per creare un modello TensorFlow Lite per predizioni di viaggio
# Questo script deve essere eseguito su un computer con Python e TensorFlow installati

import tensorflow as tf
import numpy as np
import pandas as pd
from sklearn.preprocessing import StandardScaler
from sklearn.model_selection import train_test_split
import json

def create_trip_prediction_model():
    """
    Crea un modello semplice per predire i viaggi futuri
    """

    # Simula dati di training (sostituire con dati reali)
    # Features: [num_trips, trips_per_month, avg_duration, avg_distance,
    #           current_month, season, trip_type_encoded, destination_variability,
    #           recent_trend, days_since_last_trip]

    # Genera dati sintetici per l'esempio
    np.random.seed(42)
    n_samples = 1000

    # Input features (10 dimensioni come definito nel codice Android)
    X = np.random.rand(n_samples, 10)

    # Output labels (4 dimensioni come definito nel codice Android)
    # [short_term_probability, seasonal_probability, exploration_probability, return_probability]
    y = np.random.rand(n_samples, 4)

    # Normalizza i dati
    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)

    # Split dei dati
    X_train, X_test, y_train, y_test = train_test_split(
        X_scaled, y, test_size=0.2, random_state=42
    )

    # Crea il modello
    model = tf.keras.Sequential([
        tf.keras.layers.Dense(64, activation='relu', input_shape=(10,)),
        tf.keras.layers.Dropout(0.3),
        tf.keras.layers.Dense(32, activation='relu'),
        tf.keras.layers.Dropout(0.2),
        tf.keras.layers.Dense(16, activation='relu'),
        tf.keras.layers.Dense(4, activation='sigmoid')  # 4 output con sigmoid per probabilità
    ])

    # Compila il modello
    model.compile(
        optimizer='adam',
        loss='mse',
        metrics=['mae']
    )

    # Addestra il modello
    print("Training del modello...")
    history = model.fit(
        X_train, y_train,
        epochs=50,
        batch_size=32,
        validation_data=(X_test, y_test),
        verbose=1
    )

    # Valuta il modello
    test_loss, test_mae = model.evaluate(X_test, y_test, verbose=0)
    print(f"Test Loss: {test_loss:.4f}")
    print(f"Test MAE: {test_mae:.4f}")

    return model, scaler

def convert_to_tflite(model, model_path="trip_prediction_model.tflite"):
    """
    Converte il modello Keras in TensorFlow Lite
    """
    print("Conversione a TensorFlow Lite...")

    # Converti il modello
    converter = tf.lite.TFLiteConverter.from_keras_model(model)

    # Ottimizzazioni opzionali
    converter.optimizations = [tf.lite.Optimize.DEFAULT]

    # Converti
    tflite_model = converter.convert()

    # Salva il modello
    with open(model_path, 'wb') as f:
        f.write(tflite_model)

    print(f"Modello salvato come: {model_path}")
    print(f"Dimensione del modello: {len(tflite_model) / 1024:.2f} KB")

    return tflite_model

def test_tflite_model(model_path="trip_prediction_model.tflite"):
    """
    Testa il modello TensorFlow Lite
    """
    print("Test del modello TensorFlow Lite...")

    # Carica il modello
    interpreter = tf.lite.Interpreter(model_path=model_path)
    interpreter.allocate_tensors()

    # Ottieni dettagli input/output
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    print("Input shape:", input_details[0]['shape'])
    print("Output shape:", output_details[0]['shape'])

    # Test con dati di esempio
    test_input = np.random.rand(1, 10).astype(np.float32)

    interpreter.set_tensor(input_details[0]['index'], test_input)
    interpreter.invoke()

    output_data = interpreter.get_tensor(output_details[0]['index'])
    print("Output del test:", output_data[0])

    return True

def create_improved_model_with_real_patterns():
    """
    Versione migliorata del modello che simula pattern realistici
    """
    print("Creazione modello migliorato...")

    # Simula dati più realistici
    n_samples = 2000

    # Features più realistiche
    num_trips = np.random.poisson(8, n_samples) / 50.0  # Normalizzato
    trips_per_month = np.random.gamma(2, 2, n_samples) / 10.0
    avg_duration = np.random.gamma(3, 2, n_samples) / 30.0
    avg_distance = np.random.lognormal(4, 1, n_samples) / 1000.0
    current_month = np.random.randint(0, 12, n_samples) / 11.0
    season = np.random.randint(0, 4, n_samples) / 3.0
    trip_type = np.random.choice([0.0, 0.33, 0.66, 1.0], n_samples)
    destination_var = np.random.beta(2, 5, n_samples)
    recent_trend = np.random.beta(3, 3, n_samples)
    days_since_last = np.random.exponential(30, n_samples) / 365.0

    X = np.column_stack([
        num_trips, trips_per_month, avg_duration, avg_distance,
        current_month, season, trip_type, destination_var,
        recent_trend, days_since_last
    ])

    # Output basato su logica realistica
    short_term = np.where(days_since_last * 365 > 30,
                          np.random.beta(2, 8, n_samples),
                          np.random.beta(8, 2, n_samples))

    seasonal = np.where(season > 0.5,
                        np.random.beta(6, 4, n_samples),
                        np.random.beta(3, 7, n_samples))

    exploration = destination_var * np.random.beta(3, 7, n_samples)

    return_prob = (1 - destination_var) * np.random.beta(7, 3, n_samples)

    y = np.column_stack([short_term, seasonal, exploration, return_prob])

    # Normalizza
    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)

    # Split
    X_train, X_test, y_train, y_test = train_test_split(
        X_scaled, y, test_size=0.2, random_state=42
    )

    # Modello più complesso
    model = tf.keras.Sequential([
        tf.keras.layers.Dense(128, activation='relu', input_shape=(10,)),
        tf.keras.layers.BatchNormalization(),
        tf.keras.layers.Dropout(0.4),

        tf.keras.layers.Dense(64, activation='relu'),
        tf.keras.layers.BatchNormalization(),
        tf.keras.layers.Dropout(0.3),

        tf.keras.layers.Dense(32, activation='relu'),
        tf.keras.layers.Dropout(0.2),

        tf.keras.layers.Dense(16, activation='relu'),
        tf.keras.layers.Dense(4, activation='sigmoid')
    ])

    # Compila con learning rate scheduler
    initial_learning_rate = 0.001
    lr_schedule = tf.keras.optimizers.schedules.ExponentialDecay(
        initial_learning_rate,
        decay_steps=100,
        decay_rate=0.96,
        staircase=True
    )

    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=lr_schedule),
        loss='mse',
        metrics=['mae']
    )

    # Callbacks
    early_stopping = tf.keras.callbacks.EarlyStopping(
        monitor='val_loss', patience=10, restore_best_weights=True
    )

    reduce_lr = tf.keras.callbacks.ReduceLROnPlateau(
        monitor='val_loss', factor=0.2, patience=5, min_lr=0.0001
    )

    # Training
    print("Training del modello migliorato...")
    history = model.fit(
        X_train, y_train,
        epochs=100,
        batch_size=64,
        validation_data=(X_test, y_test),
        callbacks=[early_stopping, reduce_lr],
        verbose=1
    )

    # Valutazione finale
    test_loss, test_mae = model.evaluate(X_test, y_test, verbose=0)
    print(f"Modello migliorato - Test Loss: {test_loss:.4f}, Test MAE: {test_mae:.4f}")

    # Salva anche i parametri del scaler
    scaler_params = {
        'mean': scaler.mean_.tolist(),
        'scale': scaler.scale_.tolist()
    }

    with open('scaler_params.json', 'w') as f:
        json.dump(scaler_params, f)

    return model, scaler

def main():
    """
    Funzione principale per creare e testare il modello
    """
    print("=== Creazione Modello TensorFlow per Travel Companion ===")

    # Crea modello semplice
    print("\n1. Creazione modello base...")
    model_basic, scaler_basic = create_trip_prediction_model()

    # Converti a TensorFlow Lite
    print("\n2. Conversione a TensorFlow Lite...")
    tflite_model = convert_to_tflite(model_basic,
                                     "app/src/main/assets/trip_prediction_model_basic.tflite")

    # Test del modello
    print("\n3. Test del modello...")
    test_tflite_model("app/src/main/assets/trip_prediction_model_basic.tflite")

    # Crea modello migliorato
    print("\n4. Creazione modello migliorato...")
    model_improved, scaler_improved = create_improved_model_with_real_patterns()

    # Converti modello migliorato
    print("\n5. Conversione modello migliorato...")
    convert_to_tflite(model_improved, "trip_prediction_model.tflite")

    # Test finale
    print("\n6. Test modello finale...")
    test_tflite_model("trip_prediction_model.tflite")

    print("\n=== Creazione completata! ===")
    print("File generati:")
    print("- trip_prediction_model.tflite (modello finale)")
    print("- trip_prediction_model_basic.tflite (modello base)")
    print("- scaler_params.json (parametri di normalizzazione)")
    print("\nCopia il file .tflite nella cartella assets/ della tua app Android.")

if __name__ == "__main__":
    main()

# ============================================================================
# ISTRUZIONI PER L'USO:
# ============================================================================
"""
1. Installa le dipendenze:
   pip install tensorflow scikit-learn pandas numpy

2. Esegui lo script:
   python create_model.py

3. Copia il file trip_prediction_model.tflite nella cartella 
   app/src/main/assets/ del tuo progetto Android

4. Il modello sarà automaticamente caricato dall'app Android

NOTA: Questo è un modello di esempio. Per un modello reale, dovresti:
- Raccogliere dati reali di viaggio dai tuoi utenti
- Implementare feature engineering più sofisticato
- Usare tecniche di validazione più robuste
- Considerare l'uso di modelli più complessi (RNN, LSTM, ecc.)
"""

# ============================================================================
# FEATURE ENGINEERING AVANZATO (per implementazioni future)
# ============================================================================

def advanced_feature_engineering(trips_data):
    """
    Esempio di feature engineering avanzato per migliorare le predizioni
    """
    features = {}

    # Features temporali
    features['day_of_week'] = trips_data['start_date'].dt.dayofweek
    features['is_weekend'] = (features['day_of_week'] >= 5).astype(int)
    features['month'] = trips_data['start_date'].dt.month
    features['season'] = trips_data['start_date'].dt.month.apply(
        lambda x: (x % 12 + 3) // 3
    )

    # Features geografici
    features['lat_std'] = trips_data.groupby('user_id')['latitude'].transform('std')
    features['lng_std'] = trips_data.groupby('user_id')['longitude'].transform('std')

    # Features di comportamento
    features['trips_last_30_days'] = trips_data.groupby('user_id')['start_date'].transform(
        lambda x: ((x.max() - x) <= pd.Timedelta(days=30)).sum()
    )

    # Features di sequenza
    features['days_since_last_trip'] = trips_data.groupby('user_id')['start_date'].diff().dt.days
    features['avg_gap_between_trips'] = features['days_since_last_trip'].rolling(5).mean()

    return features

# ============================================================================
# MODELLO RNN PER SEQUENZE TEMPORALI (avanzato)
# ============================================================================

def create_rnn_model():
    """
    Modello RNN più avanzato per catturare pattern temporali
    """
    model = tf.keras.Sequential([
        tf.keras.layers.LSTM(64, return_sequences=True, input_shape=(None, 10)),
        tf.keras.layers.Dropout(0.3),
        tf.keras.layers.LSTM(32, return_sequences=False),
        tf.keras.layers.Dropout(0.2),
        tf.keras.layers.Dense(16, activation='relu'),
        tf.keras.layers.Dense(4, activation='sigmoid')
    ])

    model.compile(
        optimizer='adam',
        loss='mse',
        metrics=['mae']
    )

    return model