package main;

import java.util.Random;

public class Calculos {

    private int nivel;
    private int valor1;
    private int valor2;
    private int op;
    private int resultado;


    public Calculos(int nivel) {
        Random random = new Random();
        this.op = random.nextInt(2);
        this.nivel = nivel;

        if (nivel == 1) {
            this.valor1 = random.nextInt(100);
            this.valor2 = random.nextInt(100);
        } else if (nivel == 2) {
            this.valor1 = random.nextInt(200);
            this.valor2 = random.nextInt(200);
        } else if (nivel == 3) {
            this.valor1 = random.nextInt(300);
            this.valor2 = random.nextInt(300);
        } else if (nivel == 4) {
            this.valor1 = random.nextInt(400);
            this.valor2 = random.nextInt(400);
        }  else if (nivel == 5) {
            this.valor1 = random.nextInt(500);
            this.valor2 = random.nextInt(500);
        }else if (nivel == 6) {
            this.valor1 = random.nextInt(600);
            this.valor2 = random.nextInt(600);
        }else if (nivel == 7) {
            this.valor1 = random.nextInt(700);
            this.valor2 = random.nextInt(700);
        }else if (nivel == 8) {
            this.valor1 = random.nextInt(800);
            this.valor2 = random.nextInt(800);
        }else if (nivel == 9) {
            this.valor1 = random.nextInt(900);
            this.valor2 = random.nextInt(900);
        }else if (nivel == 10) {
            this.valor1 = random.nextInt(1000);
            this.valor2 = random.nextInt(1000);
        } else if (nivel == 11) {
            this.valor1 = random.nextInt(10000);
            this.valor2 = random.nextInt(10000);
        }else {
            this.valor1 = random.nextInt(100000);
            this.valor2 = random.nextInt(100000);
        }
    }


    public int getNivel() {
        return nivel;
    }

    public int getValor1() {
        return valor1;
    }

    public int getValor2() {
        return valor2;
    }

    public int getOp() {
        return op;
    }

    public int getResultado() {
        return resultado;
    }

    public String toString() {
        String opcao;
        if (this.getOp() == 0) {
            opcao = "Somar";
        } else if (this.getOp() == 1) {
            opcao = "Subtrair";

        } else if (this.getOp() == 2) {
            opcao = "Multiplicar";

        } else if (this.getOp() == 3) {
            opcao = "Dividir";

        }else {
            opcao = "Opção Inválida";
        }
        return "valor 1: " +this.getValor1()
                + "\nValor 2: " +this.getValor2()
                + "\nOperação: " +opcao
                + "\nDificuldade: " + this.getNivel();
    }
    public boolean somar(int valor){
        this.resultado = this.getValor1() + this.getValor2();
        boolean acerto = false;

        if(valor == this.getResultado()) {
            System.out.println("Resposta Correta, Parabénss.");
            acerto = true;
        }else{
            System.out.println("Vocé errou, volte quando estudar mais um pouco");
        }
        System.out.println(this.getValor1() + " + " + this.getValor2() + " + " + this.getResultado());
        return acerto;
    }

    public boolean subtrair(int valor){
        this.resultado = this.getValor1() - this.getValor2();
        boolean acerto = false;

        if(valor == this.getResultado()) {
            System.out.println("Resposta Correta, Parabénss.");
            acerto = true;
        }else{
            System.out.println("Vocé errou, volte quando estudar mais um pouco");
        }
        System.out.println(this.getValor1() + " - " + this.getValor2() + " - " + this.getResultado());
        return acerto;
    }

    public boolean multiplicar(int valor){
        this.resultado = this.getValor1() * this.getValor2();
        boolean acerto = false;

        if(valor == this.getResultado()) {
            System.out.println("Resposta Correta, Parabénss.");
            acerto = true;
        }else{
            System.out.println("Vocé errou, volte quando estudar mais um pouco");
        }
        System.out.println(this.getValor1() + " * " + this.getValor2() + " * " + this.getResultado());
        return acerto;
    }

    public boolean dividir(int valor){
        this.resultado = this.getValor1() / this.getValor2();
        boolean acerto = false;

        if(valor == this.getResultado()) {
            System.out.println("Resposta Correta, Parabénss.");
            acerto = true;
        }else{
            System.out.println("Vocé errou, volte quando estudar mais um pouco");
        }
        System.out.println(this.getValor1() + " / " + this.getValor2() + " / " + this.getResultado());
        return acerto;
    }

}

