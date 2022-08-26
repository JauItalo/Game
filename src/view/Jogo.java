package view;

import main.Calculos;

import java.util.Scanner;

public class Jogo {
    static Scanner input = new Scanner(System.in);
    static int pontos = 0;
    static Calculos calculos;

    public static void main(String[]args){
        Jogo.play();
    }
    public static void play(){
        System.out.println("Informe o nivel da dificuldade desejado [ DE 1 A 12]: ");
        int nivel = Jogo.input.nextInt();

        Jogo.calculos = new Calculos(nivel);

        System.out.println("Informe o resultado para a seguinte oporação: ");


        //soma
        if(calculos.getOp() == 0){
            System.out.println(calculos.getValor1() + " + " + calculos.getValor2());
            int resposta = Jogo.input.nextInt();

            if(calculos.somar(resposta)) {
                Jogo.pontos += 1;
                System.out.println("Você tem" + Jogo.pontos + "ponto(s).");
            }
        }

        //subtrair
        if(calculos.getOp() == 1){
            System.out.println(calculos.getValor1() + " - " + calculos.getValor2());
            int resposta = Jogo.input.nextInt();

            if(calculos.subtrair(resposta)) {
                Jogo.pontos += 1;
                System.out.println("Você tem" + Jogo.pontos + "ponto(s).");
            }
        }

        //multiplicar
        if(calculos.getOp() == 2){
            System.out.println(calculos.getValor1() + " * " + calculos.getValor2());
            int resposta = Jogo.input.nextInt();

            if(calculos.multiplicar(resposta)) {
                Jogo.pontos += 1;
                System.out.println("Você tem" + Jogo.pontos + "ponto(s).");
            }
        }

        //dividir
        if(calculos.getOp() == 3){
            System.out.println(calculos.getValor1() + " / " + calculos.getValor2());
            int resposta = Jogo.input.nextInt();

            if(calculos.dividir(resposta)) {
                Jogo.pontos += 1;
                System.out.println("Você tem" + Jogo.pontos + "ponto(s).");
            }
        } else {
            System.out.println(" A operação " +calculos.getOp() + "não é reconhecida.");
        }

        System.out.println("Deseja continuar? [1 - sim, 2 - não]");
        int continuar = Jogo.input.nextInt();

        if(continuar == 1){
            Jogo.play();
        }else {
            System.out.println("Você fez" + Jogo.pontos + "ponto(s).");
            System.out.println("Obrigado por jogar, até a próxima!");
            System.exit(0);
        }
    }
}
