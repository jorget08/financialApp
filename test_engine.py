import math

def tea_to_tem_percentage(tea_percent):
    tea_dec = tea_percent / 100.0
    tem_dec = (1.0 + tea_dec) ** (1.0 / 12.0) - 1.0
    return round(tem_dec * 100.0, 4)

def generate_schedule(initial_capital, tea_percent, monthly_deposit, total_months):
    tea_dec = tea_percent / 100.0
    tem_dec = (1.0 + tea_dec) ** (1.0 / 12.0) - 1.0
    
    records = []
    current_balance = initial_capital
    total_deposited = initial_capital
    total_interest = 0.0
    
    for month in range(1, total_months + 1):
        start_bal = current_balance
        # Asumiendo depósito al inicio del ciclo
        interest = (start_bal + monthly_deposit) * tem_dec
        current_balance = round(start_bal + monthly_deposit + interest, 2)
        total_deposited += monthly_deposit
        total_interest += interest
        
        records.append({
            "mes": month,
            "saldo_inicial": round(start_bal, 2),
            "deposito": round(monthly_deposit, 2),
            "interes_mes": round(interest, 2),
            "saldo_final": current_balance
        })
        
    return {
        "tem_percent": round(tem_dec * 100, 4),
        "total_depositado": round(total_deposited, 2),
        "total_intereses": round(total_interest, 2),
        "saldo_final": current_balance,
        "records": records
    }

if __name__ == "__main__":
    print("=== TEST MOTOR INTERÉS COMPUESTO (TEA 7.5%, Inicial $1000, Aporte $200, 12 meses) ===")
    res = generate_schedule(1000.0, 7.5, 200.0, 12)
    print(f"TEM Equivalente: {res['tem_percent']}% mensual")
    print(f"Total Aportado: ${res['total_depositado']}")
    print(f"Total Intereses Generados: ${res['total_intereses']}")
    print(f"Saldo Final Acumulado: ${res['saldo_final']}")
    print("\n--- Desglose Primeros 6 Meses ---")
    for r in res["records"][:6]:
        print(f"Mes {r['mes']:2d} | Saldo Ini: ${r['saldo_inicial']:8.2f} | Depósito: ${r['deposito']:6.2f} | Interés: +${r['interes_mes']:5.2f} | Saldo Fin: ${r['saldo_final']:8.2f}")
