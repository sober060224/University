$F(x)=\int_a^x f(t)dt$  

$f(x)=\frac 1 {\sqrt{2\pi}\sigma}e^{-\frac {(x-μ)^2} {2\sigma^2}}$  
  
**标准化：**$\frac {x-μ} \sigma$~N(0,1)  
  
$Φ(0)=\frac 1 2$  
  
$P\{a<X\le b\}=Φ(b)-Φ(a)$  
  
$f(x) = \begin{cases}
x^2 &  x \geq 0 \\
-x  &  x < 0
\end{cases}$  
  
$f(x)=\begin{cases}{b-a} & a<x<b \\ 0 &  x \neq 0 \end{cases}$  
 
$\alpha, \beta, \gamma, \delta, \epsilon, \zeta, \eta, \theta,
\Gamma, \Delta, \Theta, \Lambda, \Xi, \Pi, \Sigma, \Omega$  

|分布名|0-1分布|B(n,p)二项分布|P(λ)泊松分布|U(a,b)均匀分布|E(λ)指数分布|N(μ,$\sigma^2$)正态分布|
|-|-|-|-|-|-|-|
|E(x)|p|np|λ|$\frac {a+b} 2$|$\frac 1 λ$|μ|
|D(x)|p(1-p)|np(1-p)|λ|$\frac {(b-a)^2} {12}$|$\frac 1 {λ^2}$|$\sigma^2$|  

$P(X=k)=\frac {λ^ke^{-λ}} {k!}$  

$E(x)=λe^{-λx}$  

$$P_{xy}=\frac {cov(X,Y)} {\sqrt {D(X)}{\sqrt {D(Y)}}}$$

$$cov(X,Y)=E(XY)-E(X)E(Y)$$

$$cov(Y,Y)=D(Y)$$

$$P\{|X-E(X)| \ge \epsilon \} \le \frac {D(x)} {\epsilon^2}$$

$\sum_{i=1}^nXi$ ~ $N(E(\sum_{i=1}^nXi),D(\sum_{i=1}^nXi))$  

|分布名|分布的形式|
|-|-|
|$X^2$分布|$X=X_1^2+X_2^2+...+X_n^2$，其中$X_i$ ~ $N(0,1)$,独立|
|t分布|$X=\frac {X_1} {\sqrt {X_2/n}}$ ~ $t(n)$，其中$X_1$ ~ $N(0,1)$,$X_2$ ~ $X^2(n)$，独立|
|F分布|$X$=$\frac {X_1/n_1} {X_2/n_2}$ ~ $F(n_1,n_2)$，其中$X_1$~$X^2(n_1)$，$X_2$~$X^2(n_2)$，独立|

$f(x) \ge 0 \quad \int_{-\infty}^{+\infty}f(x)dx=1 \quad P(a<X<b)=\int_a^bf(x)dx$

$E(X)= \sum x_ip_i \quad E(X)= \int_{-\infty}^{+\infty} xf(x)dx$

$Z=X+Y$  
$f_Z(z)=\int _{-\infty} ^{+\infty}f(x,z-x)dx=\int _{-\infty} ^{+\infty}f(z-y,y)dx$  
$f_Z(z)=f_X*f_Y=\int _{-\infty} ^{+\infty}f_X(x)f_Y(z-x)dx=\int _{-\infty} ^{+\infty}f_X(z-y)f_Y(y)dy$