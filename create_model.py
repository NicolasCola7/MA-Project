# ultra_compatible_model.py - Modello garantito compatibile con TensorFlow Lite
import tensorflow as tf
import numpy as np

def create_ultra_compatible_model():
    """
    Crea un modello estremamente semplice e compatibile
    """
    print("🔧 Creazione modello ultra-compatibile...")
    print(f"📦 TensorFlow versione: {tf.__version__}")

    # Modello semplicissimo - solo Dense layers basilari
    model = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(10,)),  # 10 input features
        tf.keras.layers.Dense(8, activation='relu'),   # Layer semplice
        tf.keras.layers.Dense(4, activation='sigmoid') # 4 output
    ])

    # Compila con ottimizzatore di base
    model.compile(
        optimizer='adam',
        loss='mse',
        metrics=['mae']
    )

    print("📊 Modello creato:")
    model.summary()

    # Genera dati di training semplici
    print("🎲 Generazione dati di training...")
    X_train = np.random.rand(100, 10).astype(np.float32)
    y_train = np.random.rand(100, 4).astype(np.float32)

    # Training veloce
    print("🎯 Training modello...")
    model.fit(
        X_train, y_train,
        epochs=5,
        batch_size=16,
        verbose=1
    )

    return model

def convert_with_maximum_compatibility(model):
    """
    Conversione con massima compatibilità
    """
    print("🔄 Conversione con massima compatibilità...")

    # Converter con settings ultra-conservativi
    converter = tf.lite.TFLiteConverter.from_keras_model(model)

    # NESSUNA ottimizzazione per evitare problemi
    # converter.optimizations = []  # Commenta questa linea

    # Usa solo operazioni basiche
    converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS]
    converter.target_spec.supported_types = [tf.float32]

    # Evita operazioni problematiche
    converter.allow_custom_ops = False
    converter.experimental_new_converter = True

    try:
        print("⚡ Tentativo conversione...")
        tflite_model = converter.convert()

        print("✅ Conversione riuscita!")
        return tflite_model

    except Exception as e:
        print(f"❌ Errore nella conversione: {e}")
        print("🔧 Tentativo con modello ancora più semplice...")
        return convert_minimal_model()

def convert_minimal_model():
    """
    Modello minimale garantito
    """
    print("🔧 Creazione modello minimale...")

    # Modello con un solo layer
    minimal_model = tf.keras.Sequential([
        tf.keras.layers.Dense(4, input_shape=(10,), activation='sigmoid')
    ])

    minimal_model.compile(optimizer='adam', loss='mse')

    # Training minimale
    X = np.random.rand(50, 10).astype(np.float32)
    y = np.random.rand(50, 4).astype(np.float32)
    minimal_model.fit(X, y, epochs=1, verbose=0)

    # Conversione super-semplice
    converter = tf.lite.TFLiteConverter.from_keras_model(minimal_model)
    converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS]

    tflite_model = converter.convert()
    print("✅ Modello minimale creato!")

    return tflite_model

def save_and_test_model(tflite_model, filename="trip_prediction_model.tflite"):
    """
    Salva e testa il modello
    """
    # Salva il file
    with open(filename, 'wb') as f:
        f.write(tflite_model)

    size_kb = len(tflite_model) / 1024
    print(f"💾 Modello salvato: {filename} ({size_kb:.1f} KB)")

    # Test del modello
    print("🧪 Test del modello...")
    try:
        interpreter = tf.lite.Interpreter(model_content=tflite_model)
        interpreter.allocate_tensors()

        input_details = interpreter.get_input_details()
        output_details = interpreter.get_output_details()

        print(f"📥 Input shape: {input_details[0]['shape']}")
        print(f"📤 Output shape: {output_details[0]['shape']}")
        print(f"📋 Input type: {input_details[0]['dtype']}")
        print(f"📋 Output type: {output_details[0]['dtype']}")

        # Test inference
        test_input = np.random.rand(1, 10).astype(np.float32)
        interpreter.set_tensor(input_details[0]['index'], test_input)
        interpreter.invoke()

        output = interpreter.get_tensor(output_details[0]['index'])
        print(f"🎯 Output test: {output[0]}")

        print("✅ Test completato con successo!")
        return True

    except Exception as e:
        print(f"❌ Errore nel test: {e}")
        return False

def main():
    """
    Funzione principale
    """
    print("=" * 60)
    print("🤖 CREAZIONE MODELLO ULTRA-COMPATIBILE")
    print("=" * 60)

    try:
        # Step 1: Crea modello semplice
        model = create_ultra_compatible_model()

        # Step 2: Converti con massima compatibilità
        tflite_model = convert_with_maximum_compatibility(model)

        # Step 3: Salva e testa
        success = save_and_test_model(tflite_model)

        if success:
            print("\n" + "=" * 60)
            print("🎉 SUCCESSO GARANTITO!")
            print("=" * 60)
            print("📁 File creato: trip_prediction_model.tflite")
            print("📱 Prossimi passi:")
            print("   1. Copia il file in app/src/main/assets/")
            print("   2. Rebuild l'app Android")
            print("   3. Il modello dovrebbe funzionare senza errori!")
            print("=" * 60)
        else:
            print("\n❌ Errore nel test finale")

    except Exception as e:
        print(f"\n💥 Errore fatale: {e}")
        print("💡 Prova a usare una versione più vecchia di TensorFlow:")
        print("   pip install tensorflow==2.13.0")

if __name__ == "__main__":
    main()